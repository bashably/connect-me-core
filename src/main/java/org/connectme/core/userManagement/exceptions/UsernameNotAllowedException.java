package org.connectme.core.userManagement.exceptions;

import org.springframework.web.util.HtmlUtils;

public class UsernameNotAllowedException extends Exception{

    public enum Reason {
        PROFANITY,
        SYNTAX,
        LENGTH
    }

    public UsernameNotAllowedException(final String username, final Reason reason) {
        super(String.format("the provided username '%s' cannot be accepted: %s", HtmlUtils.htmlEscape(username), reason.name()));
    }
}
