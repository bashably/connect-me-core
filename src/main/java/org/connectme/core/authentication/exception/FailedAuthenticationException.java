package org.connectme.core.authentication.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class FailedAuthenticationException extends Exception {

    public FailedAuthenticationException() {
        super("authentication failed, user is not authenticated");
    }

}
