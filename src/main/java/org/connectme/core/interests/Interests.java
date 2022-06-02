package org.connectme.core.interests;

import org.connectme.core.interests.entities.Interest;
import org.connectme.core.interests.entities.InterestTerm;
import org.connectme.core.interests.exceptions.NoInterestTermsFoundException;

import java.util.List;

/**
 * This interface declares all interactions which are required when it comes to managing interests on the platform.
 */
public interface Interests {

    /**
     * Searches for interests in database which are associated or related to the entered term.
     * The search not only goes through the description of all known interests, but also works with various languages.
     * @param term used for searching interests
     * @return list of found interest terms
     */
    List<InterestTerm> searchInterestTerms(final String term);


    /**
     * Attempt to find a term for the passed interest in a specific language. If the requested language is not available
     * the english default will be returned.
     *
     * @param interest root interest
     * @param language language code
     * @return term for interest in preferred language
     */
    InterestTerm getInterestTermInLanguage(final Interest interest, final String language) throws NoInterestTermsFoundException;
}
