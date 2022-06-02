package org.connectme.core.interests.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * This exception is thrown if an interest cannot be found
 */
@ResponseStatus(HttpStatus.NO_CONTENT)
public class NoSuchInterestException extends Exception {

    public NoSuchInterestException(final Long id) {
        super(String.format("interest with id %d does not exist", id));
    }

}
