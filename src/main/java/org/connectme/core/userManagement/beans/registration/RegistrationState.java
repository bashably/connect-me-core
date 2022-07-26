package org.connectme.core.userManagement.beans.registration;

/**
 * This enum contains all states the Registration object goes through
 */
public enum RegistrationState {

    /**
     * When created or reset, nothing is set
     */
    CREATED,

    /**
     * In Phase 1 the user passes username and password
     */
    USER_DATA_PASSED,


    /**
     * In Phase 2 the user must verify his phone number (with generated verification code)
     */
    WAITING_FOR_PHONE_NUMBER_VERIFICATION,


    /**
     * In Phase 3 the user has verified his phone number
     */
    USER_VERIFIED
}
