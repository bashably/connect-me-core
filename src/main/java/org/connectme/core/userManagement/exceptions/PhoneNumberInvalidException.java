package org.connectme.core.userManagement.exceptions;

import org.springframework.web.util.HtmlUtils;

/**
 * This exception is thrown if a passed phone number is not valid syntactically
 */
public class PhoneNumberInvalidException extends Exception {

    public PhoneNumberInvalidException(final String phoneNumber, final String reason) {
        super(String.format("passed phone number '%s' is invalid: %s", HtmlUtils.htmlEscape(phoneNumber), reason));
    }

}
