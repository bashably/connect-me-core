package org.connectme.core.interests.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.connectme.core.authentication.beans.UserAuthenticationBean;
import org.connectme.core.authentication.filter.UserAuthenticationFilter;
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
    private UserAuthenticationBean authenticationBean;

    @Autowired
    private InterestRepository interestRepository;

    /**
     * Looks up interest terms matching the users search term.
     * This API is called when the user searches for interest(-terms).
     * @param searchTerm search term of user
     * @param currentUser the currently authorized user who called this interface
     * @return list of interest terms matching the users search term
     * @author Daniel Mehlber
     */
    @GetMapping(value = "/search/term", consumes = "text/plain", produces = "application/json")
    public List<InterestTerm> searchTerms(@RequestParam("term") final String searchTerm,
                                          @RequestAttribute(UserAuthenticationFilter.HTTP_ATTR_USER) final User currentUser) {
        log.debug(String.format("user '%s' requested all terms for search term '%s'", HtmlUtils.htmlEscape(currentUser.getUsername()), HtmlUtils.htmlEscape(searchTerm)));
        // search for terms in database
        List<InterestTerm> foundTerms = interests.searchInterestTerms(searchTerm);

        log.debug(String.format("search completed: %d term(s) were found", foundTerms.size()));
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
                                                    @RequestAttribute(UserAuthenticationFilter.HTTP_ATTR_USER) final User currentUser) throws NoSuchInterestException, NoInterestTermsFoundException {
        log.debug(String.format("user '%s' requested term for interest id:%d in language code '%s'",
                HtmlUtils.htmlEscape(currentUser.getUsername()), id, HtmlUtils.htmlEscape(languageCode)));

        // fetch interest with id
        Interest interestRoot;
        try {
            interestRoot = interestRepository.findById(id).orElseThrow(() -> new NoSuchInterestException(id));
        } catch (NoSuchInterestException e) {
            // no interest root with interest id exists
            log.warn(String.format("cannot fetch interest term: there is no interest root with id:%d in database", id));
            throw e;
        }

        // fetch term for interest in requested language
        InterestTerm interestTerm;
        try {
            interestTerm = interests.getInterestTermInLanguage(interestRoot, languageCode);
        } catch (NoInterestTermsFoundException e) {
            log.warn(String.format("cannot fetch interest term: no interest term in language '%s' or default language 'en' were found",
                    HtmlUtils.htmlEscape(languageCode)));
            throw e;
        }

        return interestTerm;
    }

}
