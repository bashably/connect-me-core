package org.connectme.core.userManagement.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.util.HtmlUtils;

/**
 * This exception is thrown if a user cannot be created because the username already exists
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class UsernameAlreadyTakenException extends Exception {

    public UsernameAlreadyTakenException(final String username) {
        super(String.format("username '%s' is already in use and is not available", HtmlUtils.htmlEscape(username)));
    }
}
