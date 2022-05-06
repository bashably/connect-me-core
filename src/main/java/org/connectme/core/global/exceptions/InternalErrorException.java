package org.connectme.core.global.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * This exception is thrown if a fatal internal error occurred.
 * Example: Database error
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class InternalErrorException extends Exception {

    public InternalErrorException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
