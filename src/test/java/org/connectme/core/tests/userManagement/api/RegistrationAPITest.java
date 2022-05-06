package org.connectme.core.tests.userManagement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.connectme.core.tests.userManagement.testUtil.TestUserDataRepository;
import org.connectme.core.userManagement.UserManagement;
import org.connectme.core.userManagement.api.RegistrationAPI;
import org.connectme.core.userManagement.beans.StatefulRegistrationBean;
import org.connectme.core.userManagement.entities.PassedUserData;
import org.connectme.core.userManagement.entities.User;
import org.connectme.core.userManagement.impl.jpa.UserRepository;
import org.connectme.core.userManagement.logic.SmsPhoneNumberVerification;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
public class RegistrationAPITest {

    @SuppressWarnings("unused")
    @Autowired
    private MockMvc client;

    @SuppressWarnings("unused")
    @Autowired
    private UserManagement userManagement;

    @SuppressWarnings("unused")
    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void prepare() {
        // 1) remove all users from database that may reside in there
        userRepository.deleteAll();
    }

    /**
     * The {@link StatefulRegistrationBean} is stored in session under a specific attribute
     * name called "scopedTarget.{defined name}". The name of the session attribute is defined
     * in {@link RegistrationAPI}. Extract the object from the session.
     *
     * @param session session in which the bean is placed
     * @return instance of the {@link StatefulRegistrationBean}
     */
    private StatefulRegistrationBean extractRegistrationBeanFromSession(MockHttpSession session) {
      return (StatefulRegistrationBean) session.getAttribute("scopedTarget."+RegistrationAPI.SESSION_REGISTRATION);
    }

    /**
     * Test happy path of registration process (without any complications) via the API.
     * @throws Exception test failed
     */
    @Test
    public void happyPath() throws Exception {

        /*
         * SCENARIO: test the 'happy path' with no complications or failures.
         */

        // create mock session (must be passed in client request)
        MockHttpSession session = new MockHttpSession();

        // 1) init registration
        client.perform(post("/users/registration/init").session(session)).andExpect(status().isOk());

        // 2) send user registration data
        final PassedUserData userData = TestUserDataRepository.assembleValidPassedUserData();
        String json = new ObjectMapper().writeValueAsString(userData);

        client.perform(post("/users/registration/set/userdata")
                        .contentType("application/json")
                        .content(json)
                        .session(session))
                .andExpect(status().isOk());

        // 3) start verification process
        client.perform(post("/users/registration/start/verify")
                        .session(session))
                .andExpect(status().isOk());

        // 4) pass verification code
        StatefulRegistrationBean registrationObject = extractRegistrationBeanFromSession(session);
        String code = registrationObject.getPhoneNumberVerification().getVerificationCode();

        client.perform(post("/users/registration/verify")
                        .contentType("text/plain")
                        .content(code)
                        .session(session))
                .andExpect(status().isOk());

        User createdUser = userManagement.fetchUserByUsername(userData.getUsername());
        User expectedUser = new User(userData);
        Assertions.assertEquals(expectedUser, createdUser);
    }

    /**
     * Attempt registration with invalid/forbidden user data (via API)
     * @throws Exception test failed
     */
    @Test
    public void attemptForbiddenUserData() throws Exception {
        final PassedUserData invalidUserData = TestUserDataRepository.assembleForbiddenPassedUserData();
        final String json = new ObjectMapper().writeValueAsString(invalidUserData);
        MockHttpSession session = new MockHttpSession();

        // 1) init registration
        client.perform(post("/users/registration/init").session(session)).andExpect(status().isOk());

        // 2) attempt to set forbidden user data (this is not allowed)
        client.perform(post("/users/registration/set/userdata")
                        .contentType("application/json")
                        .content(json)
                        .session(session))
                .andExpect(status().isBadRequest());

        // 3) attempt to continue (this is not allowed)
        client.perform(post("/users/registration/start/verify")
                        .session(session))
                .andExpect(status().isForbidden());
    }

    /**
     * Test all possibilities of forbidden interactions with the registration process.
     * @throws Exception test failed
     */
    @Test
    public void attemptIllegalAccess() throws Exception {
        final PassedUserData userData = TestUserDataRepository.assembleValidPassedUserData();
        final String json = new ObjectMapper().writeValueAsString(userData);
        MockHttpSession session = new MockHttpSession();

        /*
         * 1) try to call actions without initialized registrations. Only action allowed is registration init
         * Allowed interactions:
         * - init
         * - upload user data
         */
        client.perform(post("/users/registration/start/verify").session(session))
                .andExpect(status().isForbidden());
        client.perform(post("/users/registration/verify")
                        .content("code")
                        .contentType("text/plain")
                        .session(session))
                .andExpect(status().isForbidden());

        /*
         * 2) init registration. Only action allowed is passing user data
         */
        // action
        client.perform(post("/users/registration/init").session(session))
                .andExpect(status().isOk());

        // not allowed interactions
        client.perform(post("/users/registration/start/verify").session(session))
                .andExpect(status().isForbidden());
        client.perform(post("/users/registration/verify")
                        .content("code")
                        .contentType("text/plain")
                        .session(session))
                .andExpect(status().isForbidden());

        /*
         * 3) set user data. Only action allowed is starting the verification process
         */
        // action
        client.perform(post("/users/registration/set/userdata")
                        .content(json)
                        .contentType("application/json")
                        .session(session))
                .andExpect(status().isOk());

        // not allowed interactions
        client.perform(post("/users/registration/verify")
                        .content("code")
                        .contentType("text/plain")
                        .session(session))
                .andExpect(status().isForbidden());
        client.perform(post("/users/registration/set/userdata")
                        .content(json)
                        .contentType("application/json")
                        .session(session))
                .andExpect(status().isForbidden());

        /*
         * 4) start verification process. Only action allowed is passing verification code
         */
        // action
        client.perform(post("/users/registration/start/verify").session(session))
                .andExpect(status().isOk());

        // not allowed interactions
        client.perform(post("/users/registration/set/userdata")
                        .content(json)
                        .contentType("application/json")
                        .session(session))
                .andExpect(status().isForbidden());
        client.perform(post("/users/registration/start/verify"))
                .andExpect(status().isForbidden());

        /*
         * 5) complete verification. No further actions allowed
         */
        // action
        StatefulRegistrationBean registration = extractRegistrationBeanFromSession(session);
        final String verificationCode = registration.getPhoneNumberVerification().getVerificationCode();
        client.perform(post("/users/registration/verify")
                        .content(verificationCode)
                        .contentType("text/plain")
                        .session(session))
                .andExpect(status().isOk());

        // not allowed interactions
        client.perform(post("/users/registration/set/userdata")
                        .content(json)
                        .contentType("application/json")
                        .session(session))
                .andExpect(status().isForbidden());
        client.perform(post("/users/registration/start/verify").session(session))
                .andExpect(status().isForbidden());
        client.perform(post("/users/registration/verify")
                        .content("code")
                        .contentType("text/plain")
                        .session(session))
                .andExpect(status().isForbidden());

    }

    /**
     * This helper method performs all API calls from init to verification attempts exceeded.
     * @param session the current session that is being used for registration
     * @throws Exception errors occurred during this process
     */
    private void performUntilVerificationAttemptsExceeded(MockHttpSession session) throws Exception {
        // 1) init registration
        client.perform(post("/users/registration/init").session(session)).andExpect(status().isOk());

        // 2) send user registration data
        final PassedUserData userData = TestUserDataRepository.assembleValidPassedUserData();
        String json = new ObjectMapper().writeValueAsString(userData);

        client.perform(post("/users/registration/set/userdata")
                        .contentType("application/json")
                        .content(json)
                        .session(session))
                .andExpect(status().isOk());

        StatefulRegistrationBean statefulRegistrationBean = extractRegistrationBeanFromSession(session);

        // 3) exceed the maximum amount of verification attempts
        for(int i = 0; i < SmsPhoneNumberVerification.MAX_AMOUNT_VERIFICATION_ATTEMPTS; i++) {
            // 3.1) start verification process
            client.perform(post("/users/registration/start/verify")
                            .session(session))
                    .andExpect(status().isOk());

            // 3.2) pass wrong verification code
            client.perform(post("/users/registration/verify")
                            .contentType("text/plain")
                            .content("wrong")
                            .session(session))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    public void exceedVerificationAttempts() throws Exception {
        /*
         * SCENARIO: user to exceeds the amount of verification attempts and has to wait.
         */

        // create mock session (must be passed in client request)
        MockHttpSession session = new MockHttpSession();

        // exceed verification limit => verification block
        performUntilVerificationAttemptsExceeded(session);

        // try another time (this should fail because the verification is blocked)
        client.perform(post("/users/registration/start/verify")
                        .session(session))
                .andExpect(status().isForbidden());

    }

    @Test
    public void attemptInvalidReset() throws Exception {
        /*
         * SCENARIO: user to exceeds the amount of verification attempts and has to wait, but instead
         * he tries to reset the registration in order to bypass this behavior. This has to be prevented.
         */

        // create mock session (must be passed in client request)
        MockHttpSession session = new MockHttpSession();

        // exceed verification limit => verification block
        performUntilVerificationAttemptsExceeded(session);

        // attempt to illegally reset registration (is forbidden)
        client.perform(post("/users/registration/init").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    public void usernameAlreadyTaken() throws Exception {

        /*
         * 1) create user in database with username
         */
        PassedUserData userData = TestUserDataRepository.assembleValidPassedUserData();
        userManagement.createNewUser(new User(userData));


        /*
         * 2) try to create a new user with same username using API
         */
        MockHttpSession session = new MockHttpSession();

        String json = new ObjectMapper().writeValueAsString(userData);

        // not allowed interactions
        client.perform(post("/users/registration/set/userdata")
                        .content(json)
                        .contentType("application/json")
                        .session(session))
                .andExpect(status().isConflict());
    }

    @Test
    public void attemptTakenPhoneNumber() throws Exception {
        /*
         * 1) create user in database with username
         */
        PassedUserData userData1 = TestUserDataRepository.assembleValidPassedUserData();
        userManagement.createNewUser(new User(userData1));

        // assemble userdata with different username
        PassedUserData userData2;
        do {
            userData2 = TestUserDataRepository.assembleValidPassedUserData();
        } while (userData2.getUsername().equals(userData1.getUsername()));
        // set phone number of already existing user
        userData2.setPhoneNumber(userData1.getPhoneNumber());

        /*
         * 2) try to create a new user with same username using API
         */
        MockHttpSession session = new MockHttpSession();

        String json = new ObjectMapper().writeValueAsString(userData2);

        // not allowed interactions
        client.perform(post("/users/registration/set/userdata")
                        .content(json)
                        .contentType("application/json")
                        .session(session))
                .andExpect(status().isConflict());
    }
}
