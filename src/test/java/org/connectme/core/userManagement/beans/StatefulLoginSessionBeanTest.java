package org.connectme.core.userManagement.beans;

import org.connectme.core.authentication.beans.UserAuthenticationBean;
import org.connectme.core.global.exceptions.ForbiddenInteractionException;
import org.connectme.core.interests.impl.jpa.InterestRepository;
import org.connectme.core.interests.impl.jpa.InterestTermRepository;
import org.connectme.core.interests.testUtil.InterestRepositoryTestUtil;
import org.connectme.core.userManagement.UserManagement;
import org.connectme.core.userManagement.beans.login.StatefulLoginSessionBean;
import org.connectme.core.userManagement.entities.PassedLoginData;
import org.connectme.core.userManagement.entities.PassedUserData;
import org.connectme.core.userManagement.entities.User;
import org.connectme.core.userManagement.exceptions.NoSuchUserException;
import org.connectme.core.userManagement.exceptions.VerificationAttemptNotAllowedException;
import org.connectme.core.userManagement.exceptions.WrongPasswordException;
import org.connectme.core.userManagement.exceptions.WrongVerificationCodeException;
import org.connectme.core.userManagement.impl.jpa.UserRepository;
import org.connectme.core.userManagement.beans.login.LoginState;
import org.connectme.core.userManagement.logic.SmsPhoneNumberVerificationProcess;
import org.connectme.core.userManagement.testUtil.UserRepositoryTestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class StatefulLoginSessionBeanTest {

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
    private StatefulLoginSessionBean statefulLoginSessionBean;

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
        statefulLoginSessionBean.reset();
        // pass login data of created user
        statefulLoginSessionBean.passLoginData(new PassedLoginData(user.getUsername(), user.getPasswordHash()));
        // start phone number verification
        statefulLoginSessionBean.startAndWaitForVerification();
        // pass verification code
        String code = statefulLoginSessionBean.getPhoneNumberVerification().getVerificationCode();
        statefulLoginSessionBean.checkVerificationCode(code);

        // -- assert --
        Assertions.assertEquals(LoginState.PROFILE_VERIFIED, statefulLoginSessionBean.getState());
        Assertions.assertEquals(user.getUsername(), statefulLoginSessionBean.getLoginData().getUsername());
        Assertions.assertEquals(user.getPasswordHash(), statefulLoginSessionBean.getLoginData().getPasswordHash());
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
        statefulLoginSessionBean.reset();
        Assertions.assertThrows(WrongPasswordException.class, () -> statefulLoginSessionBean.passLoginData(wrong));
        Assertions.assertEquals(LoginState.INIT, statefulLoginSessionBean.getState());

        statefulLoginSessionBean.passLoginData(correct);
        // -- assert --
        Assertions.assertEquals(LoginState.CORRECT_LOGIN_DATA_PASSED, statefulLoginSessionBean.getState());
    }

    @Test
    public void attemptExceedVerificationLimit() throws Exception {
        // -- arrange --
        PassedUserData userdata = UserRepositoryTestUtil.assembleValidPassedUserData(interestTermRepository);
        User user = userFactoryBean.build(userdata);
        userManagement.createNewUser(user);

        PassedLoginData passedLoginData = new PassedLoginData(user.getUsername(), user.getPasswordHash());

        // -- act and assert--
        statefulLoginSessionBean.reset();
        statefulLoginSessionBean.passLoginData(passedLoginData);

        // exceed attempts
        exceedVerificationAttempts();

        Assertions.assertThrows(VerificationAttemptNotAllowedException.class, () -> statefulLoginSessionBean.startAndWaitForVerification());

        // -- assert --
        Assertions.assertEquals(LoginState.CORRECT_LOGIN_DATA_PASSED, statefulLoginSessionBean.getState());
    }

    @Test
    public void attemptUnknownUsername() throws Exception {
        // -- arrange --
        PassedUserData userdata = UserRepositoryTestUtil.assembleValidPassedUserData(interestTermRepository);
        User user = userFactoryBean.build(userdata);
        userManagement.createNewUser(user);

        PassedLoginData passedLoginData = new PassedLoginData("xx"+user.getUsername()+"xx", user.getPasswordHash());

        // -- act and assert--
        statefulLoginSessionBean.reset();
        Assertions.assertThrows(NoSuchUserException.class, () -> statefulLoginSessionBean.passLoginData(passedLoginData));

        // -- assert --
        Assertions.assertEquals(LoginState.INIT, statefulLoginSessionBean.getState());
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
        statefulLoginSessionBean.reset();
        Assertions.assertThrows(ForbiddenInteractionException.class, () -> statefulLoginSessionBean.checkVerificationCode(""));
        Assertions.assertThrows(ForbiddenInteractionException.class, () -> statefulLoginSessionBean.startAndWaitForVerification());

        // pass user data
        statefulLoginSessionBean.passLoginData(passedLoginData);
        Assertions.assertThrows(ForbiddenInteractionException.class, () -> statefulLoginSessionBean.passLoginData(passedLoginData));
        Assertions.assertThrows(ForbiddenInteractionException.class, () -> statefulLoginSessionBean.checkVerificationCode(""));

        // start phone number verification
        statefulLoginSessionBean.startAndWaitForVerification();
        Assertions.assertThrows(ForbiddenInteractionException.class, () -> statefulLoginSessionBean.startAndWaitForVerification());
        Assertions.assertThrows(ForbiddenInteractionException.class, () -> statefulLoginSessionBean.passLoginData(passedLoginData));

        // enter verification code
        String code = statefulLoginSessionBean.getPhoneNumberVerification().getVerificationCode();
        statefulLoginSessionBean.checkVerificationCode(code);
        Assertions.assertThrows(ForbiddenInteractionException.class, () -> statefulLoginSessionBean.passLoginData(passedLoginData));
        Assertions.assertThrows(ForbiddenInteractionException.class, () -> statefulLoginSessionBean.checkVerificationCode(""));
        Assertions.assertThrows(ForbiddenInteractionException.class, () -> statefulLoginSessionBean.startAndWaitForVerification());

    }

    private void exceedVerificationAttempts() throws Exception {
        // exceed attempts
        for(int i = 0; i < SmsPhoneNumberVerificationProcess.MAX_AMOUNT_VERIFICATION_ATTEMPTS; i++) {
            statefulLoginSessionBean.startAndWaitForVerification();
            try {
                statefulLoginSessionBean.checkVerificationCode("");
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

        statefulLoginSessionBean.reset();
        statefulLoginSessionBean.passLoginData(passedLoginData);

        // exceed attempts
        exceedVerificationAttempts();

        // -- act and assert --
        Assertions.assertThrows(ForbiddenInteractionException.class, () -> statefulLoginSessionBean.reset());
        Assertions.assertEquals(LoginState.CORRECT_LOGIN_DATA_PASSED, statefulLoginSessionBean.getState());
    }

}
