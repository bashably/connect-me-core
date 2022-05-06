package org.connectme.core.userManagement.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.util.HtmlUtils;

/**
 * This exception is thrown if a user wants to set/change his phone number to one which is already
 * used by another user.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class PhoneNumberAlreadyInUseException extends Exception {

    public PhoneNumberAlreadyInUseException(final String phoneNumber) {
        super(String.format("the passed phone number '%s' is already in use by another user", HtmlUtils.htmlEscape(phoneNumber)));
    }

}
