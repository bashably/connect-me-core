package org.connectme.core.userManagement.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.connectme.core.global.exceptions.ForbiddenInteractionException;
import org.connectme.core.global.exceptions.InternalErrorException;
import org.connectme.core.userManagement.UserManagement;
import org.connectme.core.userManagement.beans.StatefulRegistrationBean;
import org.connectme.core.userManagement.beans.UserFactoryBean;
import org.connectme.core.userManagement.entities.PassedUserData;
import org.connectme.core.userManagement.entities.User;
import org.connectme.core.userManagement.exceptions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SuppressWarnings("unused")
@RestController
public class RegistrationAPI {

    @Autowired
    private UserManagement userManagement;

    @Autowired
    private UserFactoryBean userFactory;

    public static final String SESSION_REGISTRATION = "session-registration";

    private final Logger log = LogManager.getLogger(RegistrationAPI.class);

    @Autowired
    private StatefulRegistrationBean registration;

    /**
     * The client calls this method in order to init or reset a registration.
     *
     * @throws ForbiddenInteractionException this API call is not expected in the current registration's state
     * @author Daniel Mehlber
     */
    @PostMapping("/users/registration/init")
    public void initRegistration() throws ForbiddenInteractionException {
        log.debug("registration initialization requested");
        try {
            registration.reset();
        } catch (final ForbiddenInteractionException e) {
            log.warn("registration re-initialization currently not allowed");
            throw e;
        }
        log.info("registration (re-)initialized and reset");
    }


    /**
     * This REST endpoint is called by the client when he wants to upload all necessary user-data for the registration.
     * The user-data must be passed in JSON format and will be stored in session in order to be verified later with
     * a phone number.
     *
     * @param userData all required user data in JSON format
     * @throws ForbiddenInteractionException this API call is not expected in the registration's state
     * @throws UserDataInsufficientException the passed user data is incomplete or does not meet requirements
     * @throws InternalErrorException an unexpected and fatal internal error occurred
     * @throws UsernameAlreadyTakenException the passed username is already taken and cannot be registered
     * @author Daniel Mehlber
     */
    @PostMapping(value="/users/registration/set/userdata", consumes="application/json")
    public void uploadUserData(@RequestBody final PassedUserData userData) throws ForbiddenInteractionException, UserDataInsufficientException, InternalErrorException, UsernameAlreadyTakenException, PhoneNumberAlreadyInUseException {
        log.debug("user data for registration received");
        /*
         * setting user data in session bean (if interaction is even allowed) and checking if it is allowed by the
         * system (syntax, profanity, ...).
         *
         * The availability will be checked in the next step, not here yet.
         */
        try { // catch internal errors
            try {
                registration.setUserData(userData);
            } catch (ForbiddenInteractionException e) {
                log.warn("user data upload denied: " + e.getMessage());
                throw e;
            } catch (UserDataInsufficientException e) {
                log.warn("user data rejected: " + e.getMessage());
                throw e;
            } catch (PhoneNumberAlreadyInUseException e) {
                log.warn("user data rejected: the passed phone number is already in use");
                throw e;
            } catch (UsernameAlreadyTakenException e) {
                log.warn("user data rejected due to unavailable username: " + e.getMessage());
                throw e;
            }

        } catch (InternalErrorException e) {
            log.fatal("user data upload for registration failed due to an internal error", e);
            throw e;
        }

        log.info("user data for registration received");
    }

    /**
     * This REST endpoint is called be the client when he wants to start the phone number verification.
     * A verification code will be generated and sent to the user's passed phone number.
     *
     * @throws ForbiddenInteractionException this API call is not expected in the current registration state
     * @throws VerificationAttemptNotAllowedException a verification attempt is not allowed to the present moment
     * @author Daniel Mehlber
     */
    @PostMapping("/users/registration/start/verify")
    public void startVerificationProcess() throws ForbiddenInteractionException, VerificationAttemptNotAllowedException {
        log.debug("start of phone number verification process requested");
        try {
            // start verification process
            registration.startAndWaitForVerification();
        } catch (ForbiddenInteractionException e) {
            log.warn("start of phone number verification process denied: " + e.getMessage());
            throw e;
        } catch (VerificationAttemptNotAllowedException e) {
            log.warn("start of another verification attempt denied: " + e.getMessage());
            throw e;
        }


        // TODO: send verification code via SMS (but only if not in testing mode)
        log.info("phone number verification process started for registration");
    }

    /**
     * This API endpoint is called by the client when he passes the received verification code in order to complete
     * the phone number verification.
     *
     * @param passedVerificationCode the verification code in text/plain format, passed by the user (unchecked)
     * @throws ForbiddenInteractionException this API call is not expected in the current registration state
     * @throws WrongVerificationCodeException the passed verification code is not correct
     * @throws UserDataInsufficientException user data was rejected by database and cannot be registered
     * @author Daniel Mehlber
     */
    @PostMapping(value="/users/registration/verify", consumes="text/plain")
    public void verifyWithCode(@RequestBody final String passedVerificationCode) throws ForbiddenInteractionException, WrongVerificationCodeException, InternalErrorException, UsernameAlreadyTakenException, UserDataInsufficientException {
        log.debug("phone number verification code received");
        // verify using code
        try {
            registration.checkVerificationCode(passedVerificationCode);
        } catch (ForbiddenInteractionException e) {
            log.warn("phone number verification not allowed: " + e.getMessage());
            throw e;
        } catch (WrongVerificationCodeException e) {
            log.warn("verification unsuccessful: passed verification code is not correct");
            throw e;
        }

        log.info("phone number verification successful");
        try {
            /*
             * create user from registration data and persist him in DB
             */
            final User newUser = userFactory.build(registration.getPassedUserData());
            userManagement.createNewUser(newUser);

            log.info(String.format("registration of new user '%s' was successful", newUser.getUsername()));
        } catch (InternalErrorException e) {
            log.warn("cannot create new user due to an internal error: " + e.getMessage());
            throw e;
        } catch (UsernameAlreadyTakenException e) {
            log.warn("cannot create new user: username is already taken");
            throw e;
        } catch (UserDataInsufficientException e) {
            // the user data passed mid-registration is not accepted by the database at the end of registration
            // this should not happen! The mid-process user data checks are not sufficient (database more rules)
            log.warn("cannot complete registration and create new user: " + e.getMessage());
            log.fatal("cannot complete registration because mid-registration user data checks were not sufficient. " +
                    "This should not happen at the end of the registration process", e);
            throw e;
        }

        log.info("created new user in database successfully");

    }

}
