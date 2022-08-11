package org.connectme.core.interests.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.connectme.core.authentication.filter.UserAuthenticationFilter;
import org.connectme.core.global.exceptions.InternalErrorException;
import org.connectme.core.interests.Interests;
import org.connectme.core.interests.entities.Interest;
import org.connectme.core.interests.entities.InterestTerm;
import org.connectme.core.interests.exceptions.NoInterestTermsFoundException;
import org.connectme.core.interests.exceptions.NoSuchInterestException;
import org.connectme.core.interests.impl.jpa.InterestRepository;
import org.connectme.core.userManagement.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@RestController
@RequestMapping("/interests")
public class InterestAPI {

    private final Logger log = LogManager.getLogger(InterestAPI.class);

    @Autowired
    private Interests interests;

    @Autowired
    private InterestRepository interestRepository;

    /**
     * Looks up interest terms matching the users search term.
     * This API is called when the user searches for interest(-terms).
     * @param searchTerm search term of user
     * @param currentUser the currently authorized user who called this interface
     * @return list of interest terms matching the users search term
     * @throws InternalErrorException database error
     * @author Daniel Mehlber
     */
    @GetMapping(value = "/search/term", consumes = "text/plain", produces = "application/json")
    public List<InterestTerm> searchTerms(@RequestParam("term") final String searchTerm,
                                          @RequestAttribute(UserAuthenticationFilter.HTTP_ATTR_USER) final User currentUser) throws InternalErrorException {
        log.debug("user '{}' requested all terms for search term '{}'", HtmlUtils.htmlEscape(currentUser.getUsername()), HtmlUtils.htmlEscape(searchTerm));
        // search for terms in database
        List<InterestTerm> foundTerms;
        try {
            foundTerms = interests.searchInterestTerms(searchTerm);
        } catch (InternalErrorException e) {
            log.fatal("cannot search interest terms for search term '{}' due to an internal error: %s",
                    HtmlUtils.htmlEscape(searchTerm), e.getMessage(), e);
            throw e;
        }

        log.debug("search completed: {} term(s) were found", foundTerms.size());
        return foundTerms;
    }

    /**
     * Looks up the term for an interest in a specific language
     * @param id id of the interest root
     * @param languageCode language code such as 'en' or 'de'
     * @param currentUser the currently authorized user who called this interface
     * @return an interest term in the requested language. If the language is not provided the english default will be returned
     * @throws NoSuchInterestException no interest with passed id
     * @throws NoInterestTermsFoundException no term for interest in requested AND english (this should not happen)
     */
    @GetMapping(value = "/{id}/{lang}")
    public InterestTerm getTermOfInterestInLanguage(@PathVariable("id") final long id,
                                                    @PathVariable("lang") final String languageCode,
                                                    @RequestAttribute(UserAuthenticationFilter.HTTP_ATTR_USER) final User currentUser) throws NoSuchInterestException, NoInterestTermsFoundException, InternalErrorException {
        log.debug("user '{}' requested term for interest id:{} in language code '{}'",
                HtmlUtils.htmlEscape(currentUser.getUsername()), id, HtmlUtils.htmlEscape(languageCode));

        // fetch interest with id
        Interest interestRoot;
        try {
            interestRoot = interestRepository.findById(id).orElseThrow(() -> new NoSuchInterestException(id));
        } catch (NoSuchInterestException e) {
            // no interest root with interest id exists
            log.warn("cannot fetch interest term: there is no interest root with id:{} in database", id);
            throw e;
        }

        // fetch term for interest in requested language
        InterestTerm interestTerm;
        try {
            interestTerm = interests.getInterestTermInLanguage(interestRoot, languageCode);
        } catch (NoInterestTermsFoundException e) {
            log.warn("cannot fetch interest term: no interest term in language '{}' or default language 'en' were found",
                    HtmlUtils.htmlEscape(languageCode));
            throw e;
        } catch (InternalErrorException e) {
            log.fatal("cannot fetch interest term: an internal error occurred while fetching interest term from interest root id:{} - {}", id, e.getMessage(), e);
            throw new InternalErrorException(String.format("cannot fetch interst term of interest root id:%d", id), e);
        }

        return interestTerm;
    }

}
