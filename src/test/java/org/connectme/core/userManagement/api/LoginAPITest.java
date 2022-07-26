package org.connectme.core.userManagement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.connectme.core.authentication.beans.UserAuthenticationBean;
import org.connectme.core.interests.impl.jpa.InterestRepository;
import org.connectme.core.interests.impl.jpa.InterestTermRepository;
import org.connectme.core.interests.testUtil.InterestRepositoryTestUtil;
import org.connectme.core.userManagement.UserManagement;
import org.connectme.core.userManagement.beans.login.StatefulLoginSessionBean;
import org.connectme.core.userManagement.beans.UserFactoryBean;
import org.connectme.core.userManagement.entities.PassedLoginData;
import org.connectme.core.userManagement.entities.PassedUserData;
import org.connectme.core.userManagement.entities.User;
import org.connectme.core.userManagement.impl.jpa.UserRepository;
import org.connectme.core.userManagement.logic.SmsPhoneNumberVerificationProcess;
import org.connectme.core.userManagement.testUtil.UserRepositoryTestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class LoginAPITest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserManagement userManagement;

    @Autowired
    private UserFactoryBean userFactoryBean;

    @Autowired
    private UserAuthenticationBean authenticationBean;

    @Autowired
    private InterestTermRepository interestTermRepository;

    @Autowired
    private InterestRepository interestRepository;

    @Autowired
    private MockMvc client;

    @BeforeEach
    public void prepare() {
        userRepository.deleteAll();
        InterestRepositoryTestUtil.clearRepository(interestRepository);
        InterestRepositoryTestUtil.fillRepositoryWithTestInterests(interestRepository);
    }

    /**
     * The {@link StatefulLoginSessionBean} is stored in session under a specific attribute
     * name called "scopedTarget.{defined name}". The name of the session attribute is defined
     * in {@link org.connectme.core.userManagement.api.LoginAPI}. Extract the object from the session.
     *
     * @param session session in which the bean is placed
     * @return instance of the {@link StatefulLoginSessionBean}
     */
    private StatefulLoginSessionBean extractLoginBeanFromSession(MockHttpSession session) {
        return (StatefulLoginSessionBean) session.getAttribute("scopedTarget."+ LoginAPI.SESSION_LOGIN);
    }

    private void performUntilPhoneNumberVerification(MockHttpSession session, PassedLoginData loginData) throws Exception {
        // init login process
        client.perform(post("/users/login/init").session(session)).andExpect(status().isOk());

        // pass login data
        String json = new ObjectMapper().writeValueAsString(loginData);
        client.perform(post("/users/login/userdata").contentType("application/json").content(json).session(session))
                .andExpect(status().isOk());

    }

    private String performPhoneNumberVerification(MockHttpSession session) throws Exception {
        // extract verification code and pass it to API
        StatefulLoginSessionBean bean = extractLoginBeanFromSession(session);

        client.perform(post("/users/login/verify/start")
                        .session(session))
                .andExpect(status().isOk());

        String code = bean.getPhoneNumberVerification().getVerificationCode();

        return client.perform(post("/users/login/verify/check").session(session).contentType("text/plain").content(code))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private void exceedVerificationAttempts(MockHttpSession session) throws Exception {

        for(int i = 0; i < SmsPhoneNumberVerificationProcess.MAX_AMOUNT_VERIFICATION_ATTEMPTS; i++) {
            // 3.1) start verification process
            client.perform(post("/users/login/verify/start")
                            .session(session))
                    .andExpect(status().isOk());

            // 3.2) pass wrong verification code
            client.perform(post("/users/login/verify/check")
                            .contentType("text/plain")
                            .content("wrong")
                            .session(session))
                    .andExpect(status().isBadRequest());
        }

        client.perform(post("/users/login/verify/start").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    public void happyPath() throws Exception {
        // -- arrange --
        PassedUserData userData = UserRepositoryTestUtil.assembleValidPassedUserData(interestTermRepository);
        User user = userFactoryBean.build(userData);
        userManagement.createNewUser(user);
        PassedLoginData loginData = new PassedLoginData(user.getUsername(), user.getPasswordHash());
        MockHttpSession session = new MockHttpSession();
        // -- act --
        performUntilPhoneNumberVerification(session, loginData);
        String jwt = performPhoneNumberVerification(session);

        // -- assert --
        Assertions.assertNotNull(jwt);
        Assertions.assertDoesNotThrow(() -> authenticationBean.authenticate(jwt));
    }

    @Test
    public void attemptExceedVerificationAttempts() throws Exception {
        // -- arrange --
        PassedUserData userData = UserRepositoryTestUtil.assembleValidPassedUserData(interestTermRepository);
        User user = userFactoryBean.build(userData);
        userManagement.createNewUser(user);
        PassedLoginData loginData = new PassedLoginData(user.getUsername(), user.getPasswordHash());
        MockHttpSession session = new MockHttpSession();

        // -- act --
        performUntilPhoneNumberVerification(session, loginData);
        exceedVerificationAttempts(session);

        // -- assert --
        StatefulLoginSessionBean bean = extractLoginBeanFromSession(session);
        Assertions.assertFalse(bean.getPhoneNumberVerification().isVerified());
        Assertions.assertFalse(bean.getPhoneNumberVerification().isVerificationAttemptCurrentlyAllowed());

        // wind blocker timer forward, removes block
        bean.getPhoneNumberVerification().setLastVerificationAttempt(LocalDateTime.now().minusMinutes(SmsPhoneNumberVerificationProcess.BLOCK_FAILED_ATTEMPT_MINUTES));
        Assertions.assertTrue(bean.getPhoneNumberVerification().isVerificationAttemptCurrentlyAllowed());

        Assertions.assertDoesNotThrow(() -> performPhoneNumberVerification(session));
    }

    @Test
    public void attemptIllegalAccess() throws Exception {
        // -- arrange --
        PassedUserData userData = UserRepositoryTestUtil.assembleValidPassedUserData(interestTermRepository);
        User user = userFactoryBean.build(userData);
        userManagement.createNewUser(user);
        PassedLoginData loginData = new PassedLoginData(user.getUsername(), user.getPasswordHash());
        String loginDataJson = new ObjectMapper().writeValueAsString(loginData);
        MockHttpSession session = new MockHttpSession();

        // -- act and assert --
        // init login process
        client.perform(post("/users/login/init").session(session)).andExpect(status().isOk());
        client.perform(post("/users/login/verify/start").session(session)).andExpect(status().isForbidden());
        client.perform(post("/users/login/verify/check").session(session).contentType("text/plain").content("123"))
                .andExpect(status().isForbidden());

        // pass user data
        client.perform(post("/users/login/userdata").session(session).content(loginDataJson).contentType("application/json"))
                .andExpect(status().isOk());
        client.perform(post("/users/login/verify/check").session(session).contentType("text/plain").content("123"))
                .andExpect(status().isForbidden());
        client.perform(post("/users/login/userdata").session(session).content(loginDataJson).contentType("application/json"))
                .andExpect(status().isForbidden());

        // start phone number verification
        client.perform(post("/users/login/verify/start").session(session)).andExpect(status().isOk());
        client.perform(post("/users/login/verify/start").session(session)).andExpect(status().isForbidden());
        client.perform(post("/users/login/userdata").session(session).content(loginDataJson).contentType("application/json"))
                .andExpect(status().isForbidden());

        // pass check verification code
        String code = extractLoginBeanFromSession(session).getPhoneNumberVerification().getVerificationCode();
        client.perform(post("/users/login/verify/check").session(session).contentType("text/plain").content(code)).andExpect(status().isOk());
        client.perform(post("/users/login/verify/check").session(session).contentType("text/plain").content("123"))
                .andExpect(status().isForbidden());
        client.perform(post("/users/login/verify/start").session(session)).andExpect(status().isForbidden());
        client.perform(post("/users/login/userdata").session(session).content(loginDataJson).contentType("application/json"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void attemptWrongPassword() throws Exception {
        // -- arrange --
        PassedUserData userData = UserRepositoryTestUtil.assembleValidPassedUserData(interestTermRepository);
        User user = userFactoryBean.build(userData);
        userManagement.createNewUser(user);
        PassedLoginData loginData = new PassedLoginData(user.getUsername(), "incorrect-password-hash");
        String loginDataJson = new ObjectMapper().writeValueAsString(loginData);
        MockHttpSession session = new MockHttpSession();

        client.perform(post("/users/login/init").session(session)).andExpect(status().isOk());
        client.perform(post("/users/login/userdata").session(session).content(loginDataJson).contentType("application/json"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void attemptUnknownUsername() throws Exception {
        // -- arrange --
        PassedUserData userData = UserRepositoryTestUtil.assembleValidPassedUserData(interestTermRepository);
        User user = userFactoryBean.build(userData);
        userManagement.createNewUser(user);
        PassedLoginData loginData = new PassedLoginData("unknownUser", user.getPasswordHash());
        String loginDataJson = new ObjectMapper().writeValueAsString(loginData);
        MockHttpSession session = new MockHttpSession();

        client.perform(post("/users/login/init").session(session)).andExpect(status().isOk());
        client.perform(post("/users/login/userdata").session(session).content(loginDataJson).contentType("application/json"))
                .andExpect(status().isUnauthorized());
    }

}
