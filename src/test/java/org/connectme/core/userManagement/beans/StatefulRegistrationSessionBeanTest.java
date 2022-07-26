package org.connectme.core.userManagement.beans;

import org.connectme.core.global.exceptions.ForbiddenInteractionException;
import org.connectme.core.global.exceptions.InternalErrorException;
import org.connectme.core.interests.impl.jpa.InterestRepository;
import org.connectme.core.interests.impl.jpa.InterestTermRepository;
import org.connectme.core.interests.testUtil.InterestRepositoryTestUtil;
import org.connectme.core.userManagement.beans.registration.StatefulRegistrationSessionBean;
import org.connectme.core.userManagement.entities.PassedUserData;
import org.connectme.core.userManagement.exceptions.*;
import org.connectme.core.userManagement.impl.jpa.UserRepository;
import org.connectme.core.userManagement.beans.registration.RegistrationState;
import org.connectme.core.userManagement.logic.SmsPhoneNumberVerificationProcess;
import org.connectme.core.userManagement.testUtil.UserRepositoryTestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

@SpringBootTest
public class StatefulRegistrationSessionBeanTest {

    @Autowired
    private InterestTermRepository interestTermRepository;

    @Autowired
    private InterestRepository interestRepository;

    @Autowired
    private StatefulRegistrationSessionBean statefulRegistrationSessionBean;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void prepare() {
        userRepository.deleteAll();
        InterestRepositoryTestUtil.clearRepository(interestRepository);
        InterestRepositoryTestUtil.fillRepositoryWithTestInterests(interestRepository);
    }

    @Test
    public void happyPath() throws VerificationAttemptNotAllowedException, WrongVerificationCodeException, ForbiddenInteractionException, UserDataInsufficientException, PhoneNumberAlreadyInUseException, InternalErrorException, UsernameAlreadyTakenException {
        /*
         * SCENARIO: go through happy path of registration process
         */

        PassedUserData userData = UserRepositoryTestUtil.assembleValidPassedUserData(interestTermRepository);

        statefulRegistrationSessionBean.reset();

        statefulRegistrationSessionBean.setUserData(userData);
        Assertions.assertEquals(statefulRegistrationSessionBean.getPassedUserData(), userData);


        statefulRegistrationSessionBean.startAndWaitForVerification();

        String code = statefulRegistrationSessionBean.getPhoneNumberVerification().getVerificationCode();
        statefulRegistrationSessionBean.checkVerificationCode(code);

        Assertions.assertTrue(statefulRegistrationSessionBean.getPhoneNumberVerification().isVerified());
        Assertions.assertSame(statefulRegistrationSessionBean.getState(), RegistrationState.USER_VERIFIED);
    }

    @Test
    public void exceedVerificationLimit() throws VerificationAttemptNotAllowedException, WrongVerificationCodeException, ForbiddenInteractionException, UserDataInsufficientException, PhoneNumberAlreadyInUseException, InternalErrorException, UsernameAlreadyTakenException {
        /*
         * SCENARIO: evil or clumsy user enters wrong verification code too often and has to wait for a certain amount
         * of time. The amount of verification attempts per time has to be limited because SMS costs money.
         *
         * Test this security mechanism
         */

        statefulRegistrationSessionBean.reset();

        statefulRegistrationSessionBean.setUserData(UserRepositoryTestUtil.assembleValidPassedUserData(interestTermRepository));

        // exceed max amount of allowed verifications attempts
        for (int i = 0; i < SmsPhoneNumberVerificationProcess.MAX_AMOUNT_VERIFICATION_ATTEMPTS; i++) {
            statefulRegistrationSessionBean.startAndWaitForVerification();
            try {
                statefulRegistrationSessionBean.checkVerificationCode("");
            } catch (final WrongVerificationCodeException ignored) {}
        }

        // attempt another verification right away and expect exception
        Assertions.assertThrows(VerificationAttemptNotAllowedException.class, statefulRegistrationSessionBean::startAndWaitForVerification);

        // reduce time to wait in order to complete unit test faster
        statefulRegistrationSessionBean.getPhoneNumberVerification().setLastVerificationAttempt(LocalDateTime.now().minusMinutes(SmsPhoneNumberVerificationProcess.BLOCK_FAILED_ATTEMPT_MINUTES));

        // try again (this time with the correct code)
        statefulRegistrationSessionBean.startAndWaitForVerification();
        String code = statefulRegistrationSessionBean.getPhoneNumberVerification().getVerificationCode();
        statefulRegistrationSessionBean.checkVerificationCode(code);
    }

    @Test
    public void attemptProcessRestartWhileVerificationBlock() throws VerificationAttemptNotAllowedException, ForbiddenInteractionException, UserDataInsufficientException, PhoneNumberAlreadyInUseException, InternalErrorException, UsernameAlreadyTakenException {
        /*
         * SCENARIO: evil user tries to send infinite verification SMS in order to harm us:
         * After he attempted too many verifications he must wait. To bypass that, he tries to reset the
         * registration process. This action is not allowed while the verification is "blocked".
         *
         * Test this security mechanism
         */

        statefulRegistrationSessionBean.reset();

        statefulRegistrationSessionBean.setUserData(UserRepositoryTestUtil.assembleValidPassedUserData(interestTermRepository));

        // exceed max attempt of verifications
        for (int i = 0; i < SmsPhoneNumberVerificationProcess.MAX_AMOUNT_VERIFICATION_ATTEMPTS; i++) {
            statefulRegistrationSessionBean.startAndWaitForVerification();
            try {
                statefulRegistrationSessionBean.checkVerificationCode("");
            } catch (final WrongVerificationCodeException ignored) {}
        }

        // attempt another verification right away and expect exception
        Assertions.assertThrows(VerificationAttemptNotAllowedException.class, statefulRegistrationSessionBean::startAndWaitForVerification);

        // try to reset registration in order to illegally bypass blocked time. Must be interrupted by exception.
        Assertions.assertThrows(ForbiddenInteractionException.class, statefulRegistrationSessionBean::reset);
    }

    @Test
    public void attemptIllegalInteractionsToStates() throws WrongVerificationCodeException, VerificationAttemptNotAllowedException, ForbiddenInteractionException, UserDataInsufficientException, PhoneNumberAlreadyInUseException, InternalErrorException, UsernameAlreadyTakenException {
        /*
         * SCENARIO: In every state of the registration only certain interactions are allowed.
         *
         * Test that other interactions are not allowed
         */

        PassedUserData userData = UserRepositoryTestUtil.assembleValidPassedUserData(interestTermRepository);

        // Set state to CREATED, following interactions are not allowed:
        statefulRegistrationSessionBean.reset();
        Assertions.assertThrows(ForbiddenInteractionException.class, statefulRegistrationSessionBean::startAndWaitForVerification);
        Assertions.assertThrows(ForbiddenInteractionException.class, () -> statefulRegistrationSessionBean.checkVerificationCode(""));

        // Set state to USERNAME_PASSWORD_SET, following interactions are not allowed:
        statefulRegistrationSessionBean.setUserData(userData);
        Assertions.assertThrows(ForbiddenInteractionException.class, () -> statefulRegistrationSessionBean.setUserData(userData));
        Assertions.assertThrows(ForbiddenInteractionException.class, () -> statefulRegistrationSessionBean.checkVerificationCode(""));

        // Set state to WAITING_FOR_PHONE_NUMBER_VERIFICATION, following interactions are not allowed:
        statefulRegistrationSessionBean.startAndWaitForVerification();
        Assertions.assertThrows(ForbiddenInteractionException.class, () -> statefulRegistrationSessionBean.setUserData(userData));
        Assertions.assertThrows(ForbiddenInteractionException.class, statefulRegistrationSessionBean::startAndWaitForVerification);

        // Set state to PHONE_NUMBER_VERIFIED, following interactions are not allowed:
        statefulRegistrationSessionBean.checkVerificationCode(statefulRegistrationSessionBean.getPhoneNumberVerification().getVerificationCode());
        Assertions.assertThrows(ForbiddenInteractionException.class, () -> statefulRegistrationSessionBean.setUserData(userData));
        Assertions.assertThrows(ForbiddenInteractionException.class, statefulRegistrationSessionBean::startAndWaitForVerification);
    }

}
