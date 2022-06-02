package org.connectme.core.userManagement.beans;

import org.connectme.core.authentication.beans.UserAuthenticationBean;
import org.connectme.core.global.exceptions.InternalErrorException;
import org.connectme.core.userManagement.UserManagement;
import org.connectme.core.userManagement.entities.PassedUserData;
import org.connectme.core.userManagement.entities.User;
import org.connectme.core.userManagement.exceptions.FailedAuthenticationException;
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

@SpringBootTest
public class UserAuthenticationBeanTest {

    @Autowired
    private UserAuthenticationBean userAuthenticationBean;

    @Autowired
    private UserManagement userManagement;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void prepare() {
        userRepository.deleteAll();
    }

    @Test
    public void login() throws InternalErrorException, UserDataInsufficientException, UsernameAlreadyTakenException, NoSuchUserException, FailedAuthenticationException {
        // arrange
        PassedUserData userData = TestUserDataRepository.assembleValidPassedUserData();
        User user = new User(userData);

        userManagement.createNewUser(user);

        // act
        String jwt = userAuthenticationBean.login(user);

        // assert
        Assertions.assertEquals(user.getUsername(), userAuthenticationBean.authenticate(jwt));
    }

    @Test
    public void logout() throws InternalErrorException, UserDataInsufficientException, UsernameAlreadyTakenException, NoSuchUserException {
        // arrange
        PassedUserData userData = TestUserDataRepository.assembleValidPassedUserData();
        User user = new User(userData);

        userManagement.createNewUser(user);
        String jwt = userAuthenticationBean.login(user);

        // act
        userAuthenticationBean.logout(user);

        // assert
        Assertions.assertThrows(FailedAuthenticationException.class, () -> userAuthenticationBean.authenticate(jwt));
    }

    @Test
    public void attemptMultipleClients() throws InternalErrorException, UserDataInsufficientException, UsernameAlreadyTakenException, NoSuchUserException, FailedAuthenticationException {
        // -- arrange --
        PassedUserData userData1 = TestUserDataRepository.assembleValidPassedUserData();
        User user1 = new User(userData1);
        userManagement.createNewUser(user1);
        String jwt1 = userAuthenticationBean.login(user1);

        // -- act and assert--
        // login again and override jwt1
        String jwt2 = userAuthenticationBean.login(user1);
        userAuthenticationBean.authenticate(jwt2);
        // try to authenticate with old jwt1
        Assertions.assertThrows(FailedAuthenticationException.class, () -> userAuthenticationBean.authenticate(jwt1));
    }

    @Test
    public void reloadFromDatabase() throws InternalErrorException, UserDataInsufficientException, UsernameAlreadyTakenException {
        // -- arrange --
        // login user
        PassedUserData userData = TestUserDataRepository.assembleValidPassedUserData();
        User user = new User(userData);
        userManagement.createNewUser(user);
        String jwt = userAuthenticationBean.login(user);

        // -- act --
        // clear cache, force authentication bean to reload from database
        userAuthenticationBean.clearCache();

        // -- assert --
        // reload from database and authenticate
        Assertions.assertDoesNotThrow(() -> userAuthenticationBean.authenticate(jwt));
    }

}
