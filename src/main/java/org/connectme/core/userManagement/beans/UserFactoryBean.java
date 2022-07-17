package org.connectme.core.userManagement.beans;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.connectme.core.global.exceptions.InternalErrorException;
import org.connectme.core.interests.Interests;
import org.connectme.core.interests.entities.InterestTerm;
import org.connectme.core.interests.exceptions.NoSuchInterestTermException;
import org.connectme.core.userManagement.entities.PassedUserData;
import org.connectme.core.userManagement.entities.User;
import org.connectme.core.userManagement.exceptions.UserDataInsufficientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.ApplicationScope;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

/**
 * this bean is meant to convert instances of {@link PassedUserData} into
 * instances of {@link User}. A separate factory is necessary, because the
 * conversion requires processing, like database interactions (e.g. converting the passed set of interest-term-ids to interest-term-entities).
 * @author Daniel Mehlber
 */
@Component
@ApplicationScope
public class UserFactoryBean {

    @Autowired
    private Interests interests;

    private final Logger log = LogManager.getLogger(UserFactoryBean.class);

    /**
     * Converts data from {@link PassedUserData} into instance of entity {@link User}. The passed values will be checked
     * and invalid inputs will not be converted.
     * @param passedUserData passed user data from user input (not yet checked, but will be checked here)
     * @return entity instance of class {@link User}
     * @throws UserDataInsufficientException passedUserData contained invalid values that cannot be accepted
     * @throws InternalErrorException cannot find hashing algorithm; database error
     */
    public User build(final PassedUserData passedUserData) throws UserDataInsufficientException, InternalErrorException {
        // check passed user data
        passedUserData.check();

        // set attributes of user data
        User user = new User();
        user.setUsername(passedUserData.getUsername());
        try {
            user.setPasswordHash(hash(passedUserData.getPassword()));
        } catch (NoSuchAlgorithmException e) {
            log.error("cannot hash password of user: " + e.getMessage());
            throw new InternalErrorException("cannot find hashing algorithm for hashing user password", e);
        }
        user.setPhoneNumber(passedUserData.getPhoneNumber());

        // convert passed interest term ids into interest term entities
        Set<InterestTerm> interestTerms = new HashSet<>();
        for(Long interestTermId : passedUserData.getInterestTermIds()) {
            // fetch interest term by passed id (and check if that id is correct)
            InterestTerm term;
            try {
                term = interests.fetchInterestTerm(interestTermId);
            } catch (NoSuchInterestTermException e) {
                log.warn(String.format("cannot convert to user entity: passed interest term with id:%d does not exist", interestTermId));
                throw new UserDataInsufficientException(e);
            }

            // add fetched interest term to user profile
            interestTerms.add(term);
        }

        user.setInterestTerms(interestTerms);

        return user;
    }

    public static String hash(String password) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedHash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
        return new String(encodedHash);
    }

}
