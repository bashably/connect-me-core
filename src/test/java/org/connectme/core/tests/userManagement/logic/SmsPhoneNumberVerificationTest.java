package org.connectme.core.tests.userManagement.logic;

import org.connectme.core.userManagement.exceptions.VerificationAttemptNotAllowedException;
import org.connectme.core.userManagement.exceptions.WrongVerificationCodeException;
import org.connectme.core.userManagement.logic.SmsPhoneNumberVerification;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

public class SmsPhoneNumberVerificationTest {

    @Test
    public void happyPath() throws VerificationAttemptNotAllowedException, WrongVerificationCodeException {
        // -- arrange --
        SmsPhoneNumberVerification verification = new SmsPhoneNumberVerification();

        // -- act --
        verification.startVerificationAttempt();
        verification.checkVerificationCode(verification.getVerificationCode());

        // -- assert --
        Assertions.assertTrue(verification.isVerified());
    }

    @Test
    public void exceedVerificationAttemptLimit() throws Exception {
        // -- arrange --
        SmsPhoneNumberVerification verification = new SmsPhoneNumberVerification();

        // exceed verification limit
        for(int i = 0; i < SmsPhoneNumberVerification.MAX_AMOUNT_VERIFICATION_ATTEMPTS; i++) {
            verification.startVerificationAttempt();
            try {
                verification.checkVerificationCode("");
            } catch (WrongVerificationCodeException ignored) {}
        }

        // -- act and assert --
        Assertions.assertFalse(verification.isVerificationAttemptCurrentlyAllowed());
        Assertions.assertThrows(VerificationAttemptNotAllowedException.class, () -> verification.startVerificationAttempt());
        Assertions.assertFalse(verification.isVerified());
        // skip waiting time
        verification.setLastVerificationAttempt(LocalDateTime.now().minusMinutes(SmsPhoneNumberVerification.BLOCK_FAILED_ATTEMPT_MINUTES));
        // try again
        verification.startVerificationAttempt();
        verification.checkVerificationCode(verification.getVerificationCode());

        Assertions.assertTrue(verification.isVerified());

    }

    /**
     * Attempt to start a new verification attempt while another one is still pending.
     * This is not allowed.
     */
    @Test
    public void attemptVerificationAttemptWhilePendingVerification() throws VerificationAttemptNotAllowedException {
        // -- arrange --
        SmsPhoneNumberVerification verification = new SmsPhoneNumberVerification();
        verification.startVerificationAttempt();

        // -- act and assert --
        // another verification attempt is not allowed while another verification is pending
        Assertions.assertTrue(verification.isPendingVerificationAttempt());
        Assertions.assertFalse(verification.isVerificationAttemptCurrentlyAllowed());
        Assertions.assertThrows(VerificationAttemptNotAllowedException.class, () -> verification.startVerificationAttempt());

        // skip verification attempt duration and assert that another attempt is free now
        verification.setLastVerificationAttempt(LocalDateTime.now().minusMinutes(SmsPhoneNumberVerification.VERIFICATION_ATTEMPT_PENDING_DURATION_MINUTES));
        Assertions.assertFalse(verification.isPendingVerificationAttempt());
        Assertions.assertTrue(verification.isVerificationAttemptCurrentlyAllowed());
        Assertions.assertDoesNotThrow(() -> verification.startVerificationAttempt());
    }
}
