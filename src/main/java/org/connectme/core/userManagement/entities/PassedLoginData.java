package org.connectme.core.userManagement.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An instance of this class carries all information passed by the user in the login process.
 * Passed values are not checked and cannot be trusted. They are yet to check.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PassedLoginData {

    private final String username;

    private final String passwordHash;

    @JsonCreator
    public PassedLoginData(@JsonProperty("username") final String username,
                           @JsonProperty("passwordHash") final String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
