package org.connectme.core.userManagement.beans.registration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.connectme.core.global.exceptions.ForbiddenInteractionException;
import org.connectme.core.global.exceptions.InternalErrorException;
import org.connectme.core.userManagement.UserManagement;
import org.connectme.core.userManagement.api.RegistrationAPI;
import org.connectme.core.userManagement.entities.PassedUserData;
import org.connectme.core.userManagement.exceptions.*;
import org.connectme.core.userManagement.impl.jpa.UserRepository;
import org.connectme.core.userManagement.logic.SmsPhoneNumberVerificationProcess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.util.HtmlUtils;

/**
 * This stateful session bean represents the user registration process and ensures that all necessary steps are
 * completed in order to successfully register a new user to the system.
 *
 * This bean contains and manages
 * <ul>
 *     <li>process with steps that must be completed in the correct order</li>
 *     <li>security and user data validation</li>
 *     <li>data passed by the user</li>
 * </ul>
 *
 * @author Daniel Mehlber
 */
@Component(RegistrationAPI.SESSION_REGISTRATION)
@SessionScope
public class StatefulRegistrationSessionBean {

    private final Logger log = LogManager.getLogger(StatefulRegistrationSessionBean.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserManagement userManagement;

    private PassedUserData passedUserData;

    /**
     * Holds all data and progress associated with the two-factor phone number verification.
     */
    private SmsPhoneNumberVerificationProcess phoneNumberVerification;

    /**
     * The registration process has multiple steps along which the client updates the instances state
     * e.g. before and after phone number verification
     */
    private RegistrationState state;

    /**
     * Creates new Registration and sets state accordingly
     * @author Daniel Mehlber
     */
    public StatefulRegistrationSessionBean() {
        try {
            reset();
        } catch (ForbiddenInteractionException ignored) {}
    }

    /**
     * Checks syntax and availability of the received user data.
     *
     * @param passedUserData user data passed by the user himself. It will be checked in this step.
     * @throws ForbiddenInteractionException this instance is currently in a different state and awaits different interactions
     * @throws UserDataInsufficientException the user data was checked and declared insufficient or invalid
     * @throws PhoneNumberAlreadyInUseException the passed phone number cannot be used because it is already taken
     * @throws UsernameAlreadyTakenException the passed username is not available because it is already taken
     * @throws InternalErrorException an unexpected internal error occurred
     * @author Daniel Mehlber
     * @see PassedUserData#check()
     */
    public void setUserData(final PassedUserData passedUserData) throws ForbiddenInteractionException, UserDataInsufficientException, PhoneNumberAlreadyInUseException, UsernameAlreadyTakenException, InternalErrorException {
        // at first check if this interaction is allowed in the current state of this bean
        if(state != RegistrationState.CREATED) {
            log.warn(String.format("illegal access attempt: registration is in state %s and cannot accept user data", state.name()));
            throw new ForbiddenInteractionException(
                    String.format("registration is in state %s and cannot accept user data", state.name()));
        } else {
            // perform value check
            try {

                // check user data syntactically
                passedUserData.check();

                /*
                 * the availability of the username will be checked here.
                 * It's checked after it is confirmed to be allowed in any other ways on purpose.
                 */
                if (!userManagement.isUsernameAvailable(passedUserData.getUsername())) {
                    log.warn(String.format("user data upload failed: username '%s' is already taken and therefor not available", HtmlUtils.htmlEscape(passedUserData.getUsername())));
                    // if user data is invalid, reset to remove user data from registration
                    reset();
                    throw new UsernameAlreadyTakenException(passedUserData.getUsername());
                }

                // check if phone number is already in use by another user
                if(userRepository.existsByPhoneNumber(passedUserData.getPhoneNumber())) {
                    log.warn(String.format("user data upload failed: phone number '%s' is already in use and cannot be taken twice", HtmlUtils.htmlEscape(passedUserData.getPhoneNumber())));
                    throw new PhoneNumberAlreadyInUseException(passedUserData.getPhoneNumber());
                }

            } catch (final UserDataInsufficientException reason) {
                log.warn("provided user data was not sufficient: " + reason.getMessage());
                throw reason;
            } catch (final InternalErrorException e) {
                log.error("cannot set user data because of an unexpected internal error: " + e.getMessage());
                throw e;
            }

            log.debug("user passed user data for registration");
            this.passedUserData = passedUserData;
            state = RegistrationState.USER_DATA_PASSED;
        }
    }


    /**
     * Invokes state in which the passed phone number must be verified by user.
     * This state generates and sets the verification code.
     *
     * PREVIOUS STEPS:
     * - phone number has been set (first attempt)
     * - previous verification has failed (new attempt)
     *
     * NEXT STEP : user has entered verification code, check it
     *
     * @throws ForbiddenInteractionException this instance is currently in a different state and awaits different interactions
     * @throws VerificationAttemptNotAllowedException another registration is currently not allowed
     * @author Daniel Mehlber
     */
    public void startAndWaitForVerification() throws ForbiddenInteractionException, VerificationAttemptNotAllowedException {
        // at first check if this interaction is allowed in the current state of this bean
        if (state != RegistrationState.USER_DATA_PASSED) {
            log.warn(String.format("illegal access attempt: registration is in state %s and cannot start phone number verification process",
                    state.name()));
            throw new ForbiddenInteractionException(
                    String.format("registration is in state %s and cannot wait for phone number verification", state.name()));
        }

        // try to start verification attempt (may fail)
        phoneNumberVerification.startVerificationAttempt();
        state = RegistrationState.WAITING_FOR_PHONE_NUMBER_VERIFICATION;
        log.debug("phone number verification attempt started");

    }


    /**
     * Checks if a process restart (=reset of the registration object) is allowed at the present moment.
     * @return if the reset is allowed.
     * @author Daniel Mehlber
     */
    private boolean isResetAllowed() {
        /*
         * SCENARIO: hacker/user attempted to many failed phone number verifications and now has to wait
         * a certain amount of time. He now tries to circumvent this mechanism by restarting the entire registration process
         * (=resetting this registration object).
         * We cannot allow infinite verification attempts because 1 Verification = 1 SMS = Money.
         *
         * If a verification attempt is currently not allowed the user must wait and a reset is not allowed.
         */
        if(phoneNumberVerification != null)
            return phoneNumberVerification.isVerificationAttemptCurrentlyAllowed();
        else
            return true;
    }


    /**
     * This checks the verification code passed by the user and sets the state accordingly.
     *
     * PREVIOUS STEP: verification code has been generated
     * NEXT STEPS:
     *  - if correct verification code: profile has been verified
     *  - if incorrect verification code: repeat verification process
     *
     * @param passedVerificationCode verification code that was passed by the user and needs to be checked
     * @throws ForbiddenInteractionException this instance is currently in a different state and awaits different interactions
     * @throws WrongVerificationCodeException wrong verification code passed by user
     * @author Daniel Mehlber
     */
    public void checkVerificationCode(final String passedVerificationCode) throws ForbiddenInteractionException, WrongVerificationCodeException {
        // at first check if this interaction is allowed in the current state of this bean
        if(state != RegistrationState.WAITING_FOR_PHONE_NUMBER_VERIFICATION) {
            log.warn(String.format("illegal access attempt: registration is in state %s and cannot check verification code", state.name()));
            throw new ForbiddenInteractionException(
                    String.format("registration is in state %s and cannot accept verification code", state.name()));
        }
        else {
            // check verification code
            try {
                phoneNumberVerification.checkVerificationCode(passedVerificationCode);
            } catch (final WrongVerificationCodeException e) {
                // CASE: wrong verification code, user must reenter verification process
                log.warn(String.format("phone number verification failed: user passed incorrect verification code '%s'", passedVerificationCode));
                state = RegistrationState.USER_DATA_PASSED;
                throw e;
            }

            // CASE: correct verification code has been entered
            log.debug("phone number verification code check successful");
            state = RegistrationState.USER_VERIFIED;
        }

    }

    /**
     * This resets the object stored in the session and is called.
     *
     * IMPORTANT: This must be called if the registration process is (re-)starting, but there already is
     * a registration object stored in session. In certain states a reset is not allowed at the present moment
     * (e.g. too many failed verification attempts: the user must wait a certain amount of time)
     *
     * @throws ForbiddenInteractionException cannot reset registration object right now.
     * @author Daniel Mehlber
     * @see StatefulRegistrationSessionBean#isResetAllowed()
     */
    public void reset() throws ForbiddenInteractionException {
        if(isResetAllowed()) {
            phoneNumberVerification = new SmsPhoneNumberVerificationProcess();
            state = RegistrationState.CREATED;
            passedUserData = null;
        } else {
            log.warn("illegal reset attempt: resetting the registration is currently not allowed");
            throw new ForbiddenInteractionException("a reset is currently not allowed/blocked");
        }

        log.info("a new registration has been initialized or reset");
    }

    public PassedUserData getPassedUserData() {
        return passedUserData;
    }

    public RegistrationState getState() {
        return state;
    }

    public SmsPhoneNumberVerificationProcess getPhoneNumberVerification() {
        return phoneNumberVerification;
    }

}
