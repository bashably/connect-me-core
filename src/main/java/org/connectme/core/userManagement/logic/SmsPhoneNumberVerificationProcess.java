package org.connectme.core.userManagement.logic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.connectme.core.userManagement.exceptions.VerificationAttemptNotAllowedException;
import org.connectme.core.userManagement.exceptions.WrongVerificationCodeException;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.util.Random;


/**
 * Holds all information and progress associated with the two-factor phone number verification via SMS. This process will be
 * used by multiple processes like the login-process or the registration-process
 */
public class SmsPhoneNumberVerificationProcess {

    private final Logger log = LogManager.getLogger(SmsPhoneNumberVerificationProcess.class);

    /**
     * The maximum amount of verifications a user can attempt sequentially.
     * If the user used all his attempts he must wait for {@value BLOCK_FAILED_ATTEMPT_MINUTES} Minutes before
     * he can try again.
     */
    public static final int MAX_AMOUNT_VERIFICATION_ATTEMPTS = 3;

    /**
     * If the user exceeds {@value MAX_AMOUNT_VERIFICATION_ATTEMPTS} attempts of verifying his phone number
     * he must wait this duration in minutes. This is to limit the verification attempts in total.
     */
    public static final int BLOCK_FAILED_ATTEMPT_MINUTES = 5;

    public static final int VERIFICATION_ATTEMPT_PENDING_DURATION_MINUTES = 3;

    /** generated verification code for phone number verification */
    private String verificationCode;

    /** amount of verification attempts. Used to limit amount of attempts per time */
    private int verificationAttempts;

    /** last verification attempt. Used to allow only a certain amount of attempts per time */
    private LocalDateTime lastVerificationAttempt;

    /** is registration verified */
    private boolean verified;

    public SmsPhoneNumberVerificationProcess() {
        verified = false;
        verificationAttempts = 0;
        lastVerificationAttempt = null;
    }

    /**
     * A verification attempt is pending, if the last verification attempt was started less than {@value VERIFICATION_ATTEMPT_PENDING_DURATION_MINUTES}
     * minutes ago.
     * @return true if there is a verification attempt pending
     * @author Daniel Mehlber
     */
    public boolean isPendingVerificationAttempt() {
        LocalDateTime now = LocalDateTime.now();
        return verificationCode != null && now.minusMinutes(VERIFICATION_ATTEMPT_PENDING_DURATION_MINUTES).isBefore(lastVerificationAttempt);
    }

    /**
     * <p>Checks if a verification attempt is currently allowed. This is not the case, if the user tried to verify his
     * phone number {@value MAX_AMOUNT_VERIFICATION_ATTEMPTS} times unsuccessfully. He has to wait {@value BLOCK_FAILED_ATTEMPT_MINUTES}
     * minutes before he can try again.</p>
     * <p>Another verification attempt is only allowed, if there are no other verification attempts pending for the user.</p>
     * This method makes sure that this rule is kept.
     * @return true if a verification attempt is allowed at the present moment.
     * @author Daniel Mehlber
     * @see SmsPhoneNumberVerificationProcess#isPendingVerificationAttempt()
     */
    public boolean isVerificationAttemptCurrentlyAllowed() {
        final LocalDateTime now = LocalDateTime.now();
        if(verificationAttempts >= MAX_AMOUNT_VERIFICATION_ATTEMPTS) {
            // CASE: max limit for verification attempts was exceeded
            if(lastVerificationAttempt.plusMinutes(BLOCK_FAILED_ATTEMPT_MINUTES).isBefore(now)) {
                log.debug("another verification attempt is allowed after the user was in verification block");
                // CASE: enough time has passed, allow more attempts
                verificationAttempts = 0;
                return true;
            } else {
                // CASE: not enough time has passed, prohibit another verification attempt
                log.warn("another verification attempt is currently not allowed because the user had {}/{} attempts and" +
                                " must wait for {} minutes with his last attempt at {}", verificationAttempts, MAX_AMOUNT_VERIFICATION_ATTEMPTS,
                                BLOCK_FAILED_ATTEMPT_MINUTES, lastVerificationAttempt.toString());
                return false;
            }
        } else if(isPendingVerificationAttempt()) {
            // there still is a pending verification attempt. Another attempt is currently not allowed.
            log.warn("another verification attempt is currently not allowed because there is still a pending verification process" +
                    " since {}", lastVerificationAttempt.toString());
            return false;
        } else {
            log.debug("another verification attempt is allowed");
            return true;
        }
    }

    /**
     * Randomly generates a verification code
     * @return generated verification code
     * @author Daniel Mehlber
     */
    private String generateVerificationCode() {
        return String.valueOf(new Random().nextInt(99999));
    }

    /**
     * Asserts if passedVerificationCode equals the actual verification code generated by
     * {@link SmsPhoneNumberVerificationProcess#generateVerificationCode()}.
     *
     * @param passedVerificationCode the verification code that needs to be checked
     * @throws WrongVerificationCodeException if the assertion failed and the wrong verification code has been passed.
     * @author Daniel Mehlber
     */
    public void checkVerificationCode(String passedVerificationCode) throws WrongVerificationCodeException {
        verificationAttempts++;

        if(verificationCode.equals(passedVerificationCode)) {
            log.debug("passed verification code '{}' was correct", passedVerificationCode);
            verified = true;
        } else {
            log.warn("passed verification code '{}' was wrong", HtmlUtils.htmlEscape(passedVerificationCode));
            verified = false;
            verificationCode = null;
            throw new WrongVerificationCodeException(passedVerificationCode);
        }

        // clear verification code
        verificationCode = null;
    }

    /**
     * Starts a new verification attempt after checking if this is allowed at the present moment. If a new attempt
     * is allowed, a verification code will be generated.
     *
     * @throws VerificationAttemptNotAllowedException a new verification attempt is currently not allowed
     * @author Daniel Mehlber
     * @see SmsPhoneNumberVerificationProcess#isVerificationAttemptCurrentlyAllowed()
     * @see SmsPhoneNumberVerificationProcess#generateVerificationCode()
     */
    public void startVerificationAttempt() throws VerificationAttemptNotAllowedException {
        // check if time window has passed and a new attempt is allowed
        if(isVerificationAttemptCurrentlyAllowed()) {
            // CASE: verification attempt is allowed
            this.verificationCode = generateVerificationCode();
            lastVerificationAttempt = LocalDateTime.now();
            // TODO: send verification code via SMS (but only if not in testing mode)

        } else {
            // CASE: not enough time has passed, prohibit another verification attempt
            log.warn("another verification attempt cannot be started: it is currently not allowed");
            throw new VerificationAttemptNotAllowedException();
        }
    }

    
    public String getVerificationCode() {
        return verificationCode;
    }

    public boolean isVerified() {
        return verified;
    }

    /**
     * Only for testing purposes
     * @param time new time
     */
    public void setLastVerificationAttempt(final LocalDateTime time) {
        lastVerificationAttempt = time;
    }
}
