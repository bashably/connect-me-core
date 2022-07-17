package org.connectme.core.userManagement.beans;

import org.connectme.core.authentication.beans.UserAuthenticationBean;
import org.connectme.core.authentication.exception.FailedAuthenticationException;
import org.connectme.core.global.exceptions.InternalErrorException;
import org.connectme.core.interests.impl.jpa.InterestRepository;
import org.connectme.core.interests.impl.jpa.InterestTermRepository;
import org.connectme.core.interests.testUtil.InterestRepositoryTestUtil;
import org.connectme.core.userManagement.UserManagement;
import org.connectme.core.userManagement.entities.PassedUserData;
import org.connectme.core.userManagement.entities.User;
import org.connectme.core.userManagement.exceptions.NoSuchUserException;
import org.connectme.core.userManagement.exceptions.UserDataInsufficientException;
import org.connectme.core.userManagement.exceptions.UsernameAlreadyTakenException;
import org.connectme.core.userManagement.impl.jpa.UserRepository;
import org.connectme.core.userManagement.testUtil.UserRepositoryTestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class UserAuthenticationBeanTest {

    @Autowired
    private InterestRepository interestRepository;

    @Autowired
    private InterestTermRepository interestTermRepository;

    @Autowired
    private UserAuthenticationBean userAuthenticationBean;

    @Autowired
    private UserFactoryBean userFactoryBean;

    @Autowired
    private UserManagement userManagement;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void prepare() {
        // fill interest repository
        InterestRepositoryTestUtil.clearRepository(interestRepository);
        InterestRepositoryTestUtil.fillRepositoryWithTestInterests(interestRepository);
        userRepository.deleteAll();
    }

    @Test
    public void login() throws InternalErrorException, UserDataInsufficientException, UsernameAlreadyTakenException, NoSuchUserException, FailedAuthenticationException {
        // arrange
        PassedUserData userData = UserRepositoryTestUtil.assembleValidPassedUserData(interestTermRepository);
        User user = userFactoryBean.build(userData);

        userManagement.createNewUser(user);

        // act
        String jwt = userAuthenticationBean.login(user);

        // assert
        Assertions.assertEquals(user.getUsername(), userAuthenticationBean.authenticate(jwt));
    }

    @Test
    public void logout() throws InternalErrorException, UserDataInsufficientException, UsernameAlreadyTakenException, NoSuchUserException {
        // arrange
        PassedUserData userData = UserRepositoryTestUtil.assembleValidPassedUserData(interestTermRepository);
        User user = userFactoryBean.build(userData);

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
        PassedUserData userData1 = UserRepositoryTestUtil.assembleValidPassedUserData(interestTermRepository);
        User user1 = userFactoryBean.build(userData1);
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
        PassedUserData userData = UserRepositoryTestUtil.assembleValidPassedUserData(interestTermRepository);
        User user = userFactoryBean.build(userData);
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
