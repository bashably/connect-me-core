package org.connectme.core.userManagement.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.connectme.core.authentication.filter.UserAuthenticationFilter;
import org.connectme.core.global.exceptions.InternalErrorException;
import org.connectme.core.interests.Interests;
import org.connectme.core.interests.entities.InterestTerm;
import org.connectme.core.interests.exceptions.NoSuchInterestException;
import org.connectme.core.interests.exceptions.NoSuchInterestTermException;
import org.connectme.core.userManagement.UserManagement;
import org.connectme.core.userManagement.entities.User;
import org.connectme.core.userManagement.exceptions.NoSuchUserException;
import org.connectme.core.userManagement.exceptions.NotEnoughInterestTermsProvidedException;
import org.connectme.core.userManagement.exceptions.UserDataInsufficientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Set;

/**
 * This API is meant for updating information on the user profile of the currently logged-in user.
 * @author Daniel Mehlber
 */
@RequestMapping("/users/data")
@RestController
public class UserDataAPI {

    @Autowired
    private Interests interests;

    @Autowired
    private UserManagement userManagement;

    private final Logger log = LogManager.getLogger(UserDataAPI.class);

    /**
     * Sets the interest terms of a user profile (must be at least 3).
     * @param currentUser current (by filter {@link UserAuthenticationFilter}) authenticated user. This is also the user
     *                    whose interest terms are set with this api call.
     * @param interestTermIds set of interest term ids that will be set as the new interest terms of the user profile.
     * @throws NoSuchInterestTermException one or more of the received ids do not identify an interest term
     * @throws InternalErrorException database error; runtime error; developer fault
     * @throws NotEnoughInterestTermsProvidedException at least 3 interest terms are required for the user profile
     * @author Daniel Mehlber
     */
    @PostMapping(value = "/interests/set", consumes = "application/json")
    public void setUserInterest(@RequestAttribute(UserAuthenticationFilter.HTTP_ATTR_USER) final User currentUser,
                                @RequestBody Set<Long> interestTermIds) throws NoSuchInterestTermException, InternalErrorException, NotEnoughInterestTermsProvidedException {
        log.debug(String.format("user '%s' has requested to change his interest terms to %s", currentUser.getUsername(), interestTermIds.toString()));

        // check if there are at least 3 interest terms
        if(interestTermIds.size() < 3)
            throw new NotEnoughInterestTermsProvidedException(interestTermIds.size(), 3);

        // convert ids to interest term entities
        Set<InterestTerm> interestTerms = new HashSet<>();
        try {
            for (Long id : interestTermIds) {
                interestTerms.add(interests.fetchInterestTerm(id));
            }
        } catch (NoSuchInterestTermException e) {
            log.warn("cannot add interest terms to user profile: invalid interest term id passed: {}", e.getMessage(), e);
            throw e;
        } catch (InternalErrorException e) {
            log.fatal("cannot add interest terms to user profile due to an internal error: {}", e.getMessage(), e);
            throw e;
        }

        // update user entity in database
        currentUser.setInterestTerms(interestTerms);
        try {
            userManagement.updateUserData(currentUser);
        } catch (NoSuchUserException | UserDataInsufficientException e) {
            // case: logged-in user does not exist or checked user data is not sufficient
            // note: this should not happen and is likely caused by programming mistakes
            log.fatal("cannot add interest term to user profile: cannot update user entity due to an internal error", e);
            throw new InternalErrorException("cannot add interest terms to user profile: cannot save user entity due to an internal error", e);
        }

        log.info("successfully changed interest terms of user profile");
    }

}
