package org.connectme.core.interests.exceptions;

import org.connectme.core.interests.entities.Interest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * This exception this thrown if there are no terms for an interest root that matches the request
 */
@ResponseStatus(HttpStatus.NO_CONTENT)
public class NoInterestTermsFoundException extends Exception {

    public NoInterestTermsFoundException(final Interest interest) {
        super(String.format("no terms were found for interest id:%d no matter the language", interest.getId()));
    }

}
