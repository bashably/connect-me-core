package org.connectme.core.global.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * This exception is thrown whenever the user tries to do something forbidden in a certain state or circumstance.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenInteractionException extends Exception {

    public ForbiddenInteractionException(final String message) {
        super(message);
    }

}
