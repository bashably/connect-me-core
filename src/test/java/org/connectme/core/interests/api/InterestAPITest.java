package org.connectme.core.interests.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.connectme.core.authentication.beans.UserAuthenticationBean;
import org.connectme.core.authentication.filter.UserAuthenticationFilter;
import org.connectme.core.interests.entities.Interest;
import org.connectme.core.interests.entities.InterestTerm;
import org.connectme.core.interests.impl.jpa.InterestRepository;
import org.connectme.core.interests.impl.jpa.InterestTermRepository;
import org.connectme.core.interests.testUtil.TestInterestData;
import org.connectme.core.userManagement.UserManagement;
import org.connectme.core.userManagement.entities.User;
import org.connectme.core.userManagement.impl.jpa.UserRepository;
import org.connectme.core.userManagement.testUtil.TestUserDataRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc()
public class InterestAPITest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserAuthenticationBean authenticationBean;

    @Autowired
    private InterestRepository interestRepository;

    @Autowired
    private InterestTermRepository interestTermRepository;

    @Autowired
    private UserManagement userManagement;

    private String currentJWT;

    private User currentUser;

    @Autowired
    private MockMvc client;

    @BeforeEach
    private void prepare() throws Exception {
        // fill repository
        interestRepository.deleteAll();
        interestTermRepository.deleteAll();
        TestInterestData.fillRepository(interestRepository);

        // login user (required to access api)
        userRepository.deleteAll();
        currentUser = new User(TestUserDataRepository.assembleValidPassedUserData());
        userManagement.createNewUser(currentUser);
        currentJWT = authenticationBean.login(currentUser);
    }

    /**
     * searches for interest terms by their string value. The user will do this in order to add or search for
     * interests.
     * @throws Exception unit test failed
     * @author Daniel Mehlber
     */
    @Test
    public void searchTerms() throws Exception {
        // -- arrange --
        InterestTerm term = TestInterestData.getRandomInterestTerm(interestTermRepository);

        // -- act --
        String jsonResult = client.perform(get("/interests/search/term").param("term", term.getTerm()).contentType("text/plain").header("authentication", currentJWT))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // -- assert
        List<InterestTerm> interestTerms = new ObjectMapper().readValue(jsonResult, new TypeReference<>() {
        });
        for(InterestTerm _term : interestTerms) {
            Assertions.assertEquals(term.getTerm(), _term.getTerm());
        }
    }

    /**
     * The API should return a term of an interest in the requested language.
     * In this scenario, the requested language is provided and can be fetched without problems
     * @throws Exception unit test failed
     * @author Daniel Mehlber
     */
    @Test
    public void getTermOfInterestInLanguage_languageProvided() throws Exception {
        // -- arrange --
        // add custom interest
        Interest something = new Interest();
        something.setTerms(
                new InterestTerm(something, "something", "en"),
                new InterestTerm(something, "任何事物", "ch")
        );
        something = interestRepository.save(something);

        // -- act --
        String json = client.perform(get("/interests/"+something.getId()+"/ch").header("authentication", currentJWT))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // -- assert --
        InterestTerm fetchedTerm = new ObjectMapper().readValue(json, InterestTerm.class);
        // also checking database UTF-8 here
        Assertions.assertEquals("任何事物", fetchedTerm.getTerm());
        Assertions.assertEquals("ch", fetchedTerm.getLanguageCode());
    }

    /**
     * The API should return a term of an interest in the requested language.
     * In this scenario, the requested language is not provided. In this case the international default is
     * expected ('en').
     * @throws Exception unit test failed
     * @author Daniel Mehlber
     */
    @Test
    public void getTermOfInterestInLanguage_languageNotProvided() throws Exception {
        // -- arrange --
        // add custom interest
        Interest something = new Interest();
        something.setTerms(
                new InterestTerm(something, "something", "en"),
                new InterestTerm(something, "irgendetwas", "de")
        );
        something = interestRepository.save(something);

        // -- act --
        String json = client.perform(get("/interests/"+something.getId()+"/ch").header("authentication", currentJWT))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // -- assert --
        InterestTerm fetchedTerm = new ObjectMapper().readValue(json, InterestTerm.class);
        Assertions.assertEquals("something", fetchedTerm.getTerm());
        Assertions.assertEquals("en", fetchedTerm.getLanguageCode());
    }

    /**
     * If the user tries to request a term for an interest that does not exist (id is not known)
     * the API should return HTTP Status "NO CONTENT"
     * @throws Exception the unit test failed
     */
    @Test
    public void getTermOfInterestInLanguage_interestDoesNotExist() throws Exception {
        // -- arrange --
        // search interest id that does not exist
        long interestId = 100;
        do {
            interestId++;
        } while (interestRepository.existsById(interestId));
        // -- act and assert --
        // try to fetch interest term for non-existing interest
        client.perform(get("/interests/"+interestId+"/en").header("authentication", currentJWT))
                .andExpect(status().isNoContent());
    }

    /**
     * In case the user requests an interest term in a language that is provided and the english default
     * for this interest is not set the API should return HTTP status "NO CONTENT"
     * @throws Exception unit test failed.
     */
    @Test
    public void getTermOfInterestInLanguage_noTermFound() throws Exception {
        // -- arrange --
        // add custom interest with no english default provided
        Interest something = new Interest();
        something.setTerms(
                new InterestTerm(something, "任何事物", "ch"),
                new InterestTerm(something, "irgendetwas", "de")
        );
        something = interestRepository.save(something);

        // -- act and assert --
        client.perform(get("/interests/"+something.getId()+"/xx").header("authentication", currentJWT))
                .andExpect(status().isNoContent());
    }

    /**
     * The access to the interest api is restricted. Every path to /interests/* must be protected by the {@link UserAuthenticationFilter}.
     * When there is no logged-in user or a JWT is not provided access should be forbidden.
     * @throws Exception unit test failed
     * @author Daniel Mehlber
     */
    @Test
    public void attemptUnauthorizedAccess() throws Exception {
        // -- arrange --
        authenticationBean.logout(currentUser);

        // -- act --
        // try to access anything in path /interests/*
        client.perform(get("/interests/something")).andExpect(status().isUnauthorized());
        // -- assert --
    }
}
