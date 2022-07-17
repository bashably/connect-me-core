package org.connectme.core.userManagement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.connectme.core.authentication.beans.UserAuthenticationBean;
import org.connectme.core.interests.entities.InterestTerm;
import org.connectme.core.interests.impl.jpa.InterestRepository;
import org.connectme.core.interests.impl.jpa.InterestTermRepository;
import org.connectme.core.interests.testUtil.InterestRepositoryTestUtil;
import org.connectme.core.userManagement.UserManagement;
import org.connectme.core.userManagement.beans.UserFactoryBean;
import org.connectme.core.userManagement.entities.User;
import org.connectme.core.userManagement.impl.jpa.UserRepository;
import org.connectme.core.userManagement.testUtil.UserRepositoryTestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
public class UserDataAPITest {

    @Autowired
    private MockMvc client;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserManagement userManagement;

    @Autowired
    private InterestRepository interestRepository;

    @Autowired
    private InterestTermRepository interestTermRepository;

    @Autowired
    private UserFactoryBean userFactory;

    @Autowired
    private UserAuthenticationBean userAuthenticationBean;

    private User currentUser;
    private String currentJWT;

    @BeforeEach
    public void prepare() throws Exception{
        userRepository.deleteAll();
        InterestRepositoryTestUtil.clearRepository(interestRepository);
        InterestRepositoryTestUtil.fillRepositoryWithTestInterests(interestRepository);

        // create user for each test case
        User user = userFactory.build(UserRepositoryTestUtil.assembleValidPassedUserData(interestTermRepository));
        userManagement.createNewUser(user);

        // user must be logged in
        currentJWT = userAuthenticationBean.login(user);
        currentUser = user;
    }

    @Test
    public void setUserInterests() throws Exception {
        // -- arrange --
        Set<InterestTerm> initialInterestTerms = currentUser.getInterestTerms();

        // find set of interest terms that differs from initial set
        Set<InterestTerm> newInterestTerms;
        do {
            newInterestTerms = InterestRepositoryTestUtil.getRandomInterestTerms(interestTermRepository, initialInterestTerms.size());
        } while(initialInterestTerms.equals(newInterestTerms));
        Set<Long> newInterestTermIds = newInterestTerms.stream().map(term -> term.getId()).collect(Collectors.toSet());
        String newInterestTermIdsJson = new ObjectMapper().writeValueAsString(newInterestTermIds);

        MockHttpSession session = new MockHttpSession();

        // -- act --
        client.perform(post("/users/data/interests/set")
                .contentType("application/json")
                .session(session)
                .header("authentication", currentJWT)
                .content(newInterestTermIdsJson)).andExpect(status().isOk());

        // -- assert --
        User fetchedUser = userManagement.fetchUserByUsername(currentUser.getUsername());
        Assertions.assertEquals(newInterestTerms, fetchedUser.getInterestTerms());
    }

    @Test
    public void setUserInterests_tooFewInterests() throws Exception{
        // -- arrange --
        // generate a set of 2 interest terms
        Set<InterestTerm> initialInterestTerms = currentUser.getInterestTerms();
        Set<Long> interestTerms = InterestRepositoryTestUtil.getRandomInterestTermIds(interestTermRepository, 2);
        String interestTermsJson = new ObjectMapper().writeValueAsString(interestTerms);

        // -- act --
        client.perform(post("/users/data/interests/set")
                .contentType(MediaType.APPLICATION_JSON)
                .content(interestTermsJson)
                .header("authentication", currentJWT)).andExpect(status().isBadRequest());

        // -- assert --
        User fetchedUser = userManagement.fetchUserByUsername(currentUser.getUsername());
        Assertions.assertEquals(initialInterestTerms, fetchedUser.getInterestTerms());
    }

    @Test
    public void setUserInterests_nonExistentIds() throws Exception {
        // -- arrange --
        Set<InterestTerm> initialInterestTerms = currentUser.getInterestTerms();
        // create set of 4 interest + 1 that does not exist
        Set<Long> interestTermIds = InterestRepositoryTestUtil.getRandomInterestTermIds(interestTermRepository, 4);
        Long nonExistentId = 0L;
        do {
            nonExistentId++;
        } while (interestTermRepository.existsById(nonExistentId));
        interestTermIds.add(nonExistentId);
        String interestTermsJson = new ObjectMapper().writeValueAsString(interestTermIds);

        // -- act --
        client.perform(post("/users/data/interests/set")
                .contentType(MediaType.APPLICATION_JSON)
                .content(interestTermsJson)
                .header("authentication", currentJWT)).andExpect(status().isBadRequest());

        // -- assert --
        User fetchedUser = userManagement.fetchUserByUsername(currentUser.getUsername());
        Assertions.assertEquals(initialInterestTerms, fetchedUser.getInterestTerms());
    }

}
