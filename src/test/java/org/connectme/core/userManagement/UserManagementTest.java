package org.connectme.core.userManagement;


import org.connectme.core.global.exceptions.InternalErrorException;
import org.connectme.core.userManagement.entities.PassedUserData;
import org.connectme.core.userManagement.entities.User;
import org.connectme.core.userManagement.exceptions.NoSuchUserException;
import org.connectme.core.userManagement.exceptions.UserDataInsufficientException;
import org.connectme.core.userManagement.exceptions.UsernameAlreadyTakenException;
import org.connectme.core.userManagement.impl.jpa.UserRepository;
import org.connectme.core.userManagement.testUtil.TestUserDataRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SuppressWarnings("unused")
@SpringBootTest
public class UserManagementTest {

    @BeforeEach
    public void prepare() {
        userRepository.deleteAll();
    }

    @Autowired
    private UserManagement userManagement;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testCreateUser() throws InternalErrorException, UsernameAlreadyTakenException, NoSuchUserException, UserDataInsufficientException {
        PassedUserData userdata = TestUserDataRepository.assembleValidPassedUserData();
        User user = new User(userdata);

        // persist new user
        userManagement.createNewUser(user);

        // check if user has been created
        User fetchedUser = userManagement.fetchUserByUsername(userdata.getUsername());
        Assertions.assertEquals(user, fetchedUser);

    }

    @Test
    public void testUpdateUser() throws InternalErrorException, UsernameAlreadyTakenException, NoSuchUserException, UserDataInsufficientException {
        PassedUserData originalUserData = TestUserDataRepository.assembleValidPassedUserData();
        User user = new User(originalUserData);

        // persist new user
        userManagement.createNewUser(user);

        // create new user data that is different from the original one but with same username
        PassedUserData newUserData;
        do {
            newUserData = TestUserDataRepository.assembleValidPassedUserData();
            newUserData.setUsername(originalUserData.getUsername());
        } while (originalUserData.equals(newUserData)); // repeat if user data matches until it doesn't

        // update user data accordingly
        User newUser = new User(newUserData);
        userManagement.updateUserData(newUser);

        // check if user data has been changed
        User persistedUser = userManagement.fetchUserByUsername(originalUserData.getUsername());
        Assertions.assertEquals(newUser, persistedUser);
    }

    @Test
    public void testDeleteUser() throws InternalErrorException, UsernameAlreadyTakenException, NoSuchUserException, UserDataInsufficientException {
        PassedUserData originalUserData = TestUserDataRepository.assembleValidPassedUserData();
        User user = new User(originalUserData);

        // persist new user
        userManagement.createNewUser(user);

        // delete user
        userManagement.deleteUser(user.getUsername());

        // check if the user really was deleted
        Assertions.assertThrows(NoSuchUserException.class, () -> userManagement.fetchUserByUsername(user.getUsername()));
    }

    @Test
    public void testIsUsernameAvailable() throws InternalErrorException, UsernameAlreadyTakenException, UserDataInsufficientException {
        PassedUserData originalUserData = TestUserDataRepository.assembleValidPassedUserData();
        User user = new User(originalUserData);

        // make sure that the username is available at first
        Assertions.assertTrue(userManagement.isUsernameAvailable(user.getUsername()));

        // persist new user
        userManagement.createNewUser(user);

        // make sure that the username is now not available
        Assertions.assertFalse(userManagement.isUsernameAvailable(user.getUsername()));
    }

    @Test
    public void attemptDeleteUnknownUser() throws InternalErrorException {
        /*
         * cannot delete user that does not exist. Exception is expected
         */
        String username = TestUserDataRepository.Usernames.getRandomAllowed();
        Assertions.assertTrue(userManagement.isUsernameAvailable(username));
        Assertions.assertThrows(NoSuchUserException.class, () -> userManagement.deleteUser(username));
    }

    @Test
    public void attemptCreateUserWithTakenUsername() throws InternalErrorException, UsernameAlreadyTakenException, UserDataInsufficientException {
        PassedUserData userData = TestUserDataRepository.assembleValidPassedUserData();
        User user = new User(userData);

        // create user with username once
        userManagement.createNewUser(user);

        // create user with username twice, expecting exception
        Assertions.assertThrows(UsernameAlreadyTakenException.class, () -> userManagement.createNewUser(user));
    }

    @Test
    public void attemptUpdateUnknownUser() throws InternalErrorException {
        PassedUserData userData = TestUserDataRepository.assembleValidPassedUserData();
        User user = new User(userData);

        // user does not exist, expecting exception
        Assertions.assertThrows(NoSuchUserException.class, () -> userManagement.updateUserData(user));
    }
}
