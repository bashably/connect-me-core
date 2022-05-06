package org.connectme.core.userManagement.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * this exception is thrown when the user attempts to log in but passes the wrong password
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class WrongPasswordException extends Exception {

    public WrongPasswordException() {
        super("password not correct");
    }

}
