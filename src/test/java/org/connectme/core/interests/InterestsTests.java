package org.connectme.core.interests;

import org.connectme.core.global.exceptions.InternalErrorException;
import org.connectme.core.interests.entities.Interest;
import org.connectme.core.interests.entities.InterestTerm;
import org.connectme.core.interests.impl.jpa.InterestRepository;
import org.connectme.core.interests.impl.jpa.InterestTermRepository;
import org.connectme.core.interests.testUtil.InterestRepositoryTestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Set;

@SpringBootTest
public class InterestsTests {

    @Autowired
    private Interests interestManagement;

    @Autowired
    private InterestRepository interestRepository;

    @Autowired
    private InterestTermRepository interestTermRepository;

    @BeforeEach
    private void prepare() {
        // fill repository
        InterestRepositoryTestUtil.clearRepository(interestRepository);
        InterestRepositoryTestUtil.fillRepositoryWithTestInterests(interestRepository);
    }

    /**
     * Interest terms can be searched by their content. Search a term by its value and assert that the same interest
     * term was returned.
     * @author Daniel Mehlber
     */
    @Test
    public void searchInterestTerms() throws InternalErrorException {
        // -- arrange --
        final InterestTerm interestTerm = InterestRepositoryTestUtil.getRandomInterestTerm(interestTermRepository);
        final String term = interestTerm.getTerm();

        // -- act --
        List<InterestTerm> terms = interestManagement.searchInterestTerms(term);

        // -- assert --
        for(InterestTerm fetchedTerm : terms) {
            String strTern = fetchedTerm.getTerm();
            Assertions.assertTrue(strTern.contains(term));
        }
    }

    /**
     * An interest provides multiple terms in various languages. Request a certain term in a certain language and
     * assert the returned language.
     * @throws Exception test failed
     * @author Daniel Mehlber
     */
    @Test
    public void getInterestTermInLanguage_termProvided() throws Exception {
        InterestTerm randomTerm = InterestRepositoryTestUtil.getRandomInterestTerm(interestTermRepository);
        String languageCode = randomTerm.getLanguageCode();

        Interest root = randomTerm.getRoot();

        // get term of language
        InterestTerm term = interestManagement.getInterestTermInLanguage(root, languageCode);

        // assert that this is the same term
        Assertions.assertEquals(languageCode, randomTerm.getLanguageCode());
    }

    /**
     * If an interest does not provide an interest term in the requested language it will return the english (international)
     * version. Check if this behavior works, by requesting a non-existent language and expect english term.
     * @throws Exception test failed
     * @author Daniel Mehlber
     */
    @Test
    public void getInterestTermInLanguage_termNotProvided() throws Exception {
        // -- arrange --
        Interest interest = InterestRepositoryTestUtil.getRandomInterest(interestRepository);

        Set<InterestTerm> terms = interest.getTerms();

        // -- act --
        // get interest term in language code, that certainly does not exist
        InterestTerm englishTerm = interestManagement.getInterestTermInLanguage(interest, "xx");
        //                                                  This language certainly does not exist ^^^^

        // -- assert --
        // assert that default phrase in english
        Assertions.assertEquals("en", englishTerm.getLanguageCode());
    }

    // TODO match different interest terms of the same interest root
    // TODO dont match different interests terms of different interest roots

}
