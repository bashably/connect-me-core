package org.connectme.core.userManagement.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.connectme.core.userManagement.exceptions.PasswordTooWeakException;
import org.connectme.core.userManagement.exceptions.PhoneNumberInvalidException;
import org.connectme.core.userManagement.exceptions.UsernameNotAllowedException;

import java.util.Objects;

/**
 * Contains all user data passed by the user himself during the registration process.
 * It also provides various checks for the untrustworthy user input that must be checked before use.
 *
 * The instance of this class can be converted to a {@link User} instance via its constructor.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PassedUserData {

    /** username suggestion of user (will not be reserved) */
    private String username;
    public static final int MIN_USERNAME_LENGTH = 4;
    public static final int MAX_USERNAME_LENGTH = 20;

    /** password in clear text format */
    private String password;
    public static final int MIN_PASSWORD_LENGTH = 8;

    /** telephone number of user */
    private String phoneNumber;

    /*
     * THIS CONSTRUCTOR IS MEANT TO BE PRIVATE
     * This default constructor is private because it is used to deserialize an object from JSON
     * and therefore is used only by the jackson library (which has private access to this class).
     * In any other scenario there is no use for a default constructor (with no arguments) and therefore
     * public access is prohibited.
     */
    @SuppressWarnings("unused")
    private PassedUserData() {}

    /**
     * This constructor is used to deserialize the JSON string passed by the user.
     * @param username username of user (unchecked)
     * @param password password of user (unchecked)
     * @param phoneNumber phoneNumber of user (unchecked)
     */
    @JsonCreator
    public PassedUserData(@JsonProperty(value = "username", required = true) final String username,
                          @JsonProperty(value = "password", required = true) final String password,
                          @JsonProperty(value = "phoneNumber", required = true) final String phoneNumber) {
        setUsername(username);
        setPassword(password);
        setPhoneNumber(phoneNumber);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username.trim();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password.trim();
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber.trim().replace(" ", "");
    }

    /**
     * Check if passed user data is valid and can be accepted by the system.
     *
     * @throws PasswordTooWeakException the provided password is not strong enough
     * @throws UsernameNotAllowedException the username is forbidden for various reasons
     */
    public void check() throws PasswordTooWeakException, UsernameNotAllowedException, PhoneNumberInvalidException {
        checkUsernameValue(this.username);
        checkPasswordValue(this.password, this.username);
        checkPhoneNumber(this.phoneNumber);
    }

    /**
     * Takes the passed username and checks if it's a valid username that can be accepted by the system.
     * A usernames can be {@value MIN_USERNAME_LENGTH} - {@value MAX_USERNAME_LENGTH} characters long and allows only a
     * certain subset of characters.
     *
     * @param passedUsername username to check
     * @throws UsernameNotAllowedException the username is not allowed and must not be accepted by the system
     */
    public static void checkUsernameValue(final String passedUsername) throws UsernameNotAllowedException {
        if(passedUsername.length() > MAX_USERNAME_LENGTH || passedUsername.length() < MIN_USERNAME_LENGTH)
            throw new UsernameNotAllowedException(passedUsername, UsernameNotAllowedException.Reason.LENGTH);
        else if (!passedUsername.matches("^[a-zA-Z\\d-_]*$"))
            //                                  ^^^^^^^^^^^^^^^^ allow only A-Z, a-z, 0-9, -_
            throw new UsernameNotAllowedException(passedUsername, UsernameNotAllowedException.Reason.SYNTAX);

        // TODO: Profanity Check
    }

    /**
     * Takes the passed password and checks if it's a valid password that can be accepted by the system.
     * It needs at least {@value MIN_PASSWORD_LENGTH} characters, 3 digits and must not contain the username.
     *
     * @param passedPassword the password that needs to be checked
     * @param username username that is not allowed to be contained in password
     * @throws PasswordTooWeakException the password is too weak
     */
    public static void checkPasswordValue(final String passedPassword, final String username) throws PasswordTooWeakException {

        /*
         * a password needs
         * - at least 8 characters
         * - at least 2 digits
         * - it must not contain the username
         */

        /*
         * at least MIN_PASSWORD_LENGTH amount of characters
         */
        if(passedPassword.length() < MIN_PASSWORD_LENGTH)
            throw new PasswordTooWeakException("password is too short");

        /*
         * count digits and check if there are more than 2
         */
        int digits = 0;
        for(char c : passedPassword.toCharArray()) {
            if(Character.isDigit(c))
                digits++;
        }
        if(digits < 3)
            throw new PasswordTooWeakException("password has too few digits");

        /*
         * make sure that username is not part of the password
         */
        if(passedPassword.contains(username))
            throw new PasswordTooWeakException("password contains username");
    }

    private static void checkPhoneNumber(final String phoneNumber) throws PhoneNumberInvalidException {
        if(phoneNumber.length() > 15)
            throw new PhoneNumberInvalidException(phoneNumber, "phone number is too long");

        if(!phoneNumber.matches("^\\d+$"))
            throw new PhoneNumberInvalidException(phoneNumber, "phone number does not only contain digits");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PassedUserData that = (PassedUserData) o;
        return Objects.equals(getUsername(), that.getUsername()) && Objects.equals(getPassword(), that.getPassword()) && Objects.equals(getPhoneNumber(), that.getPhoneNumber());
    }

}
