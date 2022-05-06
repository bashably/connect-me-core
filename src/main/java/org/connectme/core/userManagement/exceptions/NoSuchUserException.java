package org.connectme.core.userManagement.exceptions;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.util.HtmlUtils;

/**
 * This exception is thrown if there is no user data for a passed username
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class NoSuchUserException extends Exception {

    public NoSuchUserException(final String username) {
        super(String.format("requested user with username '%s' does not exist", HtmlUtils.htmlEscape(username)));
    }

}
