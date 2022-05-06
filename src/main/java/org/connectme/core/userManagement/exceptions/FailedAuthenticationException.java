package org.connectme.core.userManagement.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class FailedAuthenticationException extends Exception {

    public FailedAuthenticationException() {
        super("authentication failed, user is not authenticated");
    }

}
