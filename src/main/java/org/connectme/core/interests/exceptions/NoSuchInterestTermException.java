package org.connectme.core.interests.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * This exception is thrown if a certain interest term with an id has not been found or does not exist.
 * @author Daniel Mehlber
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class NoSuchInterestTermException extends Exception {

    public NoSuchInterestTermException(Long id) {
        super(String.format("no such interest term with id:%d found", id));
    }

}
