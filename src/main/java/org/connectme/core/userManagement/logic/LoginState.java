package org.connectme.core.userManagement.logic;

/**
 * States of the login process
 */
public enum LoginState {

    /**
     * 1) Login has been initialized or reset
     */
    INIT,

    /**
     * 2) User passed data necessary for login and data is correct
     */
    CORRECT_LOGIN_DATA_PASSED,

    /**
     * 3) Verification has been started and is pending
     */
    VERIFICATION_PENDING,

    /**
     * 4) Correct verification code has been passed
     */
    PROFILE_VERIFIED

}
