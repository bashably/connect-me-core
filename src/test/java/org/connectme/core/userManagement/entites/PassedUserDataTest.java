package org.connectme.core.userManagement.entites;

import org.connectme.core.userManagement.entities.PassedUserData;
import org.connectme.core.userManagement.exceptions.PasswordTooWeakException;
import org.connectme.core.userManagement.exceptions.UsernameNotAllowedException;
import org.connectme.core.userManagement.testUtil.UserRepositoryTestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PassedUserDataTest {

    /**
     * Check username syntax checker
     * @throws UsernameNotAllowedException username has been found not acceptable by system
     */
    @Test
    public void testCheckUsernamesSyntax() throws UsernameNotAllowedException {

        for(String allowed : UserRepositoryTestUtil.Usernames.allowed) {
            PassedUserData.checkUsernameValue(allowed);
        }

        for(String forbidden : UserRepositoryTestUtil.Usernames.forbidden) {
            Assertions.assertThrows(UsernameNotAllowedException.class, () -> PassedUserData.checkUsernameValue(forbidden));
        }

    }

    /**
     * Attempt usernames that are not long enough or too long.
     * @throws UsernameNotAllowedException username is not allowed, but was expected to
     */
    @Test
    public void testCheckUsernamesLength() throws UsernameNotAllowedException {

        // create string that is too short
        StringBuilder tooShortStringBuilder = new StringBuilder();
        tooShortStringBuilder.append("a".repeat(PassedUserData.MIN_USERNAME_LENGTH - 1));
        String tooShortString = tooShortStringBuilder.toString();

        // create string that is too long
        StringBuilder tooLongStringBuilder = new StringBuilder();
        tooLongStringBuilder.append("a".repeat(PassedUserData.MAX_USERNAME_LENGTH + 1));
        String tooLongString = tooLongStringBuilder.toString();

        // create string that has the right length
        int length = (PassedUserData.MIN_USERNAME_LENGTH + PassedUserData.MAX_USERNAME_LENGTH) / 2;
        StringBuilder correctStringBuilder = new StringBuilder();
        correctStringBuilder.append("a".repeat(length));
        String correctString = correctStringBuilder.toString();

        Assertions.assertThrows(UsernameNotAllowedException.class, () -> PassedUserData.checkUsernameValue(tooShortString));
        Assertions.assertThrows(UsernameNotAllowedException.class, () -> PassedUserData.checkUsernameValue(tooLongString));
        PassedUserData.checkUsernameValue(correctString);

    }

    /**
     * Test password strength checker
     * @throws PasswordTooWeakException password has been found too weak by system
     */
    @Test
    public void testCheckPassword() throws PasswordTooWeakException {

        String username = "username";
        for(String allowed : UserRepositoryTestUtil.Passwords.allowed) {
            PassedUserData.checkPasswordValue(allowed, username);
        }

        for(String forbidden : UserRepositoryTestUtil.Passwords.forbidden) {
            Assertions.assertThrows(PasswordTooWeakException.class,
                    () -> PassedUserData.checkPasswordValue(forbidden, username));
        }

    }

    /**
     * the password must not contain the username. Tests the password strength checker.
     */
    @Test
    public void attemptPasswordSameAsUsername() {
        String username = UserRepositoryTestUtil.Passwords.getRandomAllowed();
        String password = "abc" + username + "xyz";
        Assertions.assertThrows(PasswordTooWeakException.class,
                () -> PassedUserData.checkPasswordValue(password, username));
    }

}
