package org.connectme.core.userManagement.beans;

import org.connectme.core.authentication.beans.UserAuthenticationBean;
import org.connectme.core.global.exceptions.ForbiddenInteractionException;
import org.connectme.core.interests.impl.jpa.InterestRepository;
import org.connectme.core.interests.impl.jpa.InterestTermRepository;
import org.connectme.core.interests.testUtil.InterestRepositoryTestUtil;
import org.connectme.core.userManagement.UserManagement;
import org.connectme.core.userManagement.entities.PassedLoginData;
import org.connectme.core.userManagement.entities.PassedUserData;
import org.connectme.core.userManagement.entities.User;
import org.connectme.core.userManagement.exceptions.NoSuchUserException;
import org.connectme.core.userManagement.exceptions.VerificationAttemptNotAllowedException;
import org.connectme.core.userManagement.exceptions.WrongPasswordException;
import org.connectme.core.userManagement.exceptions.WrongVerificationCodeException;
import org.connectme.core.userManagement.impl.jpa.UserRepository;
import org.connectme.core.userManagement.logic.LoginState;
import org.connectme.core.userManagement.logic.SmsPhoneNumberVerification;
import org.connectme.core.userManagement.testUtil.UserRepositoryTestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class StatefulLoginBeanTest {

    @Autowired
    private InterestRepository interestRepository;

    @Autowired
    private InterestTermRepository interestTermRepository;

    @Autowired
    private UserManagement userManagement;

    @Autowired
    private UserFactoryBean userFactoryBean;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StatefulLoginBean statefulLoginBean;

    @Autowired
    private UserAuthenticationBean userAuthenticationBean;

    @BeforeEach
    public void prepare() {
        userRepository.deleteAll();
        InterestRepositoryTestUtil.clearRepository(interestRepository);
        InterestRepositoryTestUtil.fillRepositoryWithTestInterests(interestRepository);
    }

    @Test
    public void happyPath() throws Exception {
        // -- arrange --
        PassedUserData userdata = UserRepositoryTestUtil.assembleValidPassedUserData(interestTermRepository);
        User user = userFactoryBean.build(userdata);
        userManagement.createNewUser(user);

        // -- act --
        // init bean
        statefulLoginBean.reset();
        // pass login data of created user
        statefulLoginBean.passLoginData(new PassedLoginData(user.getUsername(), user.getPasswordHash()));
        // start phone number verification
        statefulLoginBean.startAndWaitForVerification();
        // pass verification code
        String code = statefulLoginBean.getPhoneNumberVerification().getVerificationCode();
        statefulLoginBean.checkVerificationCode(code);

        // -- assert --
        Assertions.assertEquals(LoginState.PROFILE_VERIFIED, statefulLoginBean.getState());
        Assertions.assertEquals(user.getUsername(), statefulLoginBean.getLoginData().getUsername());
        Assertions.assertEquals(user.getPasswordHash(), statefulLoginBean.getLoginData().getPasswordHash());
    }

    @Test
    public void attemptWrongPassword() throws Exception {
        // -- arrange --
        PassedUserData userdata = UserRepositoryTestUtil.assembleValidPassedUserData(interestTermRepository);
        User user = userFactoryBean.build(userdata);
        userManagement.createNewUser(user);


        PassedLoginData correct = new PassedLoginData(user.getUsername(), user.getPasswordHash());
        // find wrong password that does not match correct password
        String wrongPassword;
        do {
            wrongPassword = UserRepositoryTestUtil.Passwords.getRandomAllowed();
        } while (userdata.getPassword().equals(wrongPassword));
        PassedLoginData wrong = new PassedLoginData(user.getUsername(), UserFactoryBean.hash(wrongPassword));

        // -- act --
        statefulLoginBean.reset();
        Assertions.assertThrows(WrongPasswordException.class, () -> statefulLoginBean.passLoginData(wrong));
        Assertions.assertEquals(LoginState.INIT, statefulLoginBean.getState());

        statefulLoginBean.passLoginData(correct);
        // -- assert --
        Assertions.assertEquals(LoginState.CORRECT_LOGIN_DATA_PASSED, statefulLoginBean.getState());
    }

    @Test
    public void attemptExceedVerificationLimit() throws Exception {
        // -- arrange --
        PassedUserData userdata = UserRepositoryTestUtil.assembleValidPassedUserData(interestTermRepository);
        User user = userFactoryBean.build(userdata);
        userManagement.createNewUser(user);

        PassedLoginData passedLoginData = new PassedLoginData(user.getUsername(), user.getPasswordHash());

        // -- act and assert--
        statefulLoginBean.reset();
        statefulLoginBean.passLoginData(passedLoginData);

        // exceed attempts
        exceedVerificationAttempts();

        Assertions.assertThrows(VerificationAttemptNotAllowedException.class, () -> statefulLoginBean.startAndWaitForVerification());

        // -- assert --
        Assertions.assertEquals(LoginState.CORRECT_LOGIN_DATA_PASSED, statefulLoginBean.getState());
    }

    @Test
    public void attemptUnknownUsername() throws Exception {
        // -- arrange --
        PassedUserData userdata = UserRepositoryTestUtil.assembleValidPassedUserData(interestTermRepository);
        User user = userFactoryBean.build(userdata);
        userManagement.createNewUser(user);

        PassedLoginData passedLoginData = new PassedLoginData("xx"+user.getUsername()+"xx", user.getPasswordHash());

        // -- act and assert--
        statefulLoginBean.reset();
        Assertions.assertThrows(NoSuchUserException.class, () -> statefulLoginBean.passLoginData(passedLoginData));

        // -- assert --
        Assertions.assertEquals(LoginState.INIT, statefulLoginBean.getState());
    }

    @Test
    public void attemptIllegalAccess() throws Exception {
        // -- arrange --
        PassedUserData userdata = UserRepositoryTestUtil.assembleValidPassedUserData(interestTermRepository);
        User user = userFactoryBean.build(userdata);
        userManagement.createNewUser(user);

        PassedLoginData passedLoginData = new PassedLoginData(user.getUsername(), user.getPasswordHash());

        // -- act and assert --
        // init bean
        statefulLoginBean.reset();
        Assertions.assertThrows(ForbiddenInteractionException.class, () -> statefulLoginBean.checkVerificationCode(""));
        Assertions.assertThrows(ForbiddenInteractionException.class, () -> statefulLoginBean.startAndWaitForVerification());

        // pass user data
        statefulLoginBean.passLoginData(passedLoginData);
        Assertions.assertThrows(ForbiddenInteractionException.class, () -> statefulLoginBean.passLoginData(passedLoginData));
        Assertions.assertThrows(ForbiddenInteractionException.class, () -> statefulLoginBean.checkVerificationCode(""));

        // start phone number verification
        statefulLoginBean.startAndWaitForVerification();
        Assertions.assertThrows(ForbiddenInteractionException.class, () -> statefulLoginBean.startAndWaitForVerification());
        Assertions.assertThrows(ForbiddenInteractionException.class, () -> statefulLoginBean.passLoginData(passedLoginData));

        // enter verification code
        String code = statefulLoginBean.getPhoneNumberVerification().getVerificationCode();
        statefulLoginBean.checkVerificationCode(code);
        Assertions.assertThrows(ForbiddenInteractionException.class, () -> statefulLoginBean.passLoginData(passedLoginData));
        Assertions.assertThrows(ForbiddenInteractionException.class, () -> statefulLoginBean.checkVerificationCode(""));
        Assertions.assertThrows(ForbiddenInteractionException.class, () -> statefulLoginBean.startAndWaitForVerification());

    }

    private void exceedVerificationAttempts() throws Exception {
        // exceed attempts
        for(int i = 0; i < SmsPhoneNumberVerification.MAX_AMOUNT_VERIFICATION_ATTEMPTS; i++) {
            statefulLoginBean.startAndWaitForVerification();
            try {
                statefulLoginBean.checkVerificationCode("");
            } catch (WrongVerificationCodeException ignored) {}
        }
    }

    @Test
    public void attemptForbiddenReset() throws Exception {
        // -- arrange --
        PassedUserData userdata = UserRepositoryTestUtil.assembleValidPassedUserData(interestTermRepository);
        User user = userFactoryBean.build(userdata);
        userManagement.createNewUser(user);

        PassedLoginData passedLoginData = new PassedLoginData(user.getUsername(), user.getPasswordHash());

        statefulLoginBean.reset();
        statefulLoginBean.passLoginData(passedLoginData);

        // exceed attempts
        exceedVerificationAttempts();

        // -- act and assert --
        Assertions.assertThrows(ForbiddenInteractionException.class, () -> statefulLoginBean.reset());
        Assertions.assertEquals(LoginState.CORRECT_LOGIN_DATA_PASSED, statefulLoginBean.getState());
    }

}
