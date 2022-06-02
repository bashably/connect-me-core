package org.connectme.core.userManagement.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.connectme.core.authentication.beans.UserAuthenticationBean;
import org.connectme.core.global.exceptions.ForbiddenInteractionException;
import org.connectme.core.global.exceptions.InternalErrorException;
import org.connectme.core.userManagement.UserManagement;
import org.connectme.core.userManagement.beans.StatefulLoginBean;

import org.connectme.core.userManagement.entities.PassedLoginData;
import org.connectme.core.userManagement.entities.User;
import org.connectme.core.userManagement.exceptions.NoSuchUserException;
import org.connectme.core.userManagement.exceptions.VerificationAttemptNotAllowedException;
import org.connectme.core.userManagement.exceptions.WrongPasswordException;
import org.connectme.core.userManagement.exceptions.WrongVerificationCodeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoginAPI {

    public static final String SESSION_LOGIN = "session-login";

    private final Logger log = LogManager.getLogger(LoginAPI.class);

    @Autowired
    private StatefulLoginBean statefulLoginBean;

    @Autowired
    private UserManagement userManagement;

    @Autowired
    private UserAuthenticationBean authenticationUserBean;

    /**
     * This REST endpoint is called by the client if a login process is started or restarted.
     * No data is required when calling this endpoint.
     *
     * @throws ForbiddenInteractionException this API call is not expected in the current login state
     * @author Daniel Mehlber
     */
    @PostMapping("/users/login/init")
    public void init() throws ForbiddenInteractionException {
        log.debug("reset of login process requested...");
        try {
            statefulLoginBean.reset();
        } catch (ForbiddenInteractionException e) {
            log.warn("cannot re-initialize login process: " + e.getMessage());
            throw e;
        }
        log.info("successfully (re-)initialized login process");
    }

    /**
     * This API endpoint is called by the client when he passes login data (e.g. username and password(-hash)). The passed
     * login data will be checked in order to be verified with the phone number in the next step.
     *
     * @param passedLoginData login data in JSON format
     * @throws NoSuchUserException passed username is not known or associated with a valid user
     * @throws InternalErrorException an unexpected and fatal internal error occurred
     * @throws ForbiddenInteractionException this API call is not expected in the current login state
     * @throws WrongPasswordException the passed password is not correct
     * @author Daniel Mehlber
     */
    @PostMapping("/users/login/userdata")
    public void receiveUserData(@RequestBody final PassedLoginData passedLoginData) throws NoSuchUserException, InternalErrorException, ForbiddenInteractionException, WrongPasswordException {
        log.debug("received login data from user");
        try {
            statefulLoginBean.passLoginData(passedLoginData);
        } catch (NoSuchUserException e) {
            log.warn("cannot accept login data due to unknown username" + e.getMessage());
            throw e;
        } catch (InternalErrorException e) {
            log.fatal("cannot accept passed login data due to an unexpected internal error" , e);
            throw e;
        } catch (ForbiddenInteractionException e) {
            log.warn("cannot accept login data in current state: " + e.getMessage());
            throw e;
        } catch (WrongPasswordException e) {
            log.warn("cannot accept login data due to wrong password: " + e.getMessage());
            throw e;
        }

        log.info("received and accepted correct login data");
    }

    /**
     * This REST endpoint is called by the user when he wants to start the phone number verification process. If this is
     * currently allowed, a verification code will be generated and sent to the users phone number.
     *
     * @throws ForbiddenInteractionException this API call is not expected in the current login state
     * @throws VerificationAttemptNotAllowedException another verification attempt is currently not allowed
     * @author Daniel Mehlber
     */
    @PostMapping("/users/login/verify/start")
    public void startPhoneNumberVerification() throws ForbiddenInteractionException, VerificationAttemptNotAllowedException {
        log.debug("login phone number verification requested...");
        try {
            statefulLoginBean.startAndWaitForVerification();
        } catch (ForbiddenInteractionException e) {
            log.warn("cannot start login phone number verification due to wrong state: " + e.getMessage());
            throw e;
        } catch (VerificationAttemptNotAllowedException e) {
            log.warn("cannot start login phone number verification: " + e.getMessage());
            throw e;
        }

        log.info("started login phone number verification process");
    }

    @PostMapping(value = "/users/login/verify/check", consumes = "text/plain", produces = "text/plain")
    public String receiveVerificationCode(@RequestBody final String passedVerificationCode) throws ForbiddenInteractionException, WrongVerificationCodeException, InternalErrorException {
        log.debug("received phone number verification code for login...");

        // check verification code
        try {
            statefulLoginBean.checkVerificationCode(passedVerificationCode);
        } catch (ForbiddenInteractionException e) {
            log.warn("cannot check phone number verification code due to wrong login state: " + e.getMessage());
            throw e;
        } catch (WrongVerificationCodeException e) {
            log.warn("phone number verification failed due to incorrect code: " + e.getMessage());
            throw e;
        }

        log.debug("phone number verification completed successfully");

        // phone number verification was successful: log in user
        String jwtToken;
        try {
            User user = userManagement.fetchUserByUsername(statefulLoginBean.getLoginData().getUsername());
            jwtToken = authenticationUserBean.login(user);
        } catch (InternalErrorException | NoSuchUserException e) {
            log.fatal("cannot login verified user due to an fatal internal error: " + e.getMessage());
            throw new InternalErrorException("cannot login verified user", e);
        }

        log.debug("user has been logged-in successfully");

        return jwtToken;
    }

}
