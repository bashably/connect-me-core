package org.connectme.core.userManagement.exceptions;

/**
 * This exception is thrown if the suggested password is not strong enough and cannot be accepted
 * because of security reasons
 */
public class PasswordTooWeakException extends Exception {

    public PasswordTooWeakException(final String reason) {
        super("the password provided is not strong enough and was rejected: " + reason);
    }

}
