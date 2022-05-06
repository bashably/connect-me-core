package org.connectme.core.userManagement.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * This exception is thrown if the passed user data cannot be accepted by the system for some reason
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UserDataInsufficientException extends Exception {

    public UserDataInsufficientException(final Throwable cause) {
        super("The passed user data cannot be accepted by the system", cause);
    }

}
