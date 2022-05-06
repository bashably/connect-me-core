package org.connectme.core.userManagement.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.util.HtmlUtils;

/**
 * This exception is thrown if the user passed the wrong verification code
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class WrongVerificationCodeException extends Exception {

    public WrongVerificationCodeException(final String wrongVerificationCode) {
        super(String.format("passed verification code '%s' is not correct", HtmlUtils.htmlEscape(wrongVerificationCode)));
    }
}
