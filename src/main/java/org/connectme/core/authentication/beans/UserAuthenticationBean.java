package org.connectme.core.authentication.beans;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.connectme.core.global.exceptions.InternalErrorException;
import org.connectme.core.userManagement.UserManagement;
import org.connectme.core.userManagement.entities.User;
import org.connectme.core.authentication.exception.FailedAuthenticationException;
import org.connectme.core.userManagement.exceptions.NoSuchUserException;
import org.connectme.core.userManagement.exceptions.UserDataInsufficientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * <p>This application scope bean checks if a request is coming from a logged-in user.
 * Inside the Rest HTTP request resides the JWT Token. It contains the username and the authentication token of
 * the claimed user. This bean then fetches the user data associated with the claimed username (including the actual authentication token)
 * and compares the claimed authentication token with the actual/real one. If they equal, the request is coming from a logged-in user.</p>
 *
 * <p>The authentication token exists because only 1 active client is allowed per user. In every registration process a new
 * authentication token is generated that replaces the old one (which renders the old JWT token of the old client obsolete/invalid).</p>
 *
 * <p>To reduce performance costs, this bean works with a cache in order to minimize the requests that are sent to the database
 * and improve overall performance. If the server reloads it can easily fetch the auth-tokens from the database
 * without forcing the users to log in again.</p>
 */
@Component
@Scope("singleton")
public class UserAuthenticationBean {

    private final Logger log = LogManager.getLogger(UserAuthenticationBean.class);

    @Autowired
    private UserManagement userManagement;

    /**
     * Caches auth tokens (for better performance). This Bean mainly works with the cache, so the database is only
     * accessed if the server restarted and the cache is gone.
     */
    private final Map<String, String> loggedInUsersCache = new HashMap<>();

    @SuppressWarnings("SpellCheckingInspection")
    private static final String SECRET = "6a8c00720cbcbd95e3acc3c5a04345ed";

    /**
     * Login user by generating new authentication token and assembling a new JWT token
     * @param user user that needs to be logged in.
     * @return new JWT token that can be used by the client in the future
     * @throws InternalErrorException system encountered unexpected errors that cannot be handled
     * @author Daniel Mehlber
     */
    public String login(final User user) throws InternalErrorException {
        // generate new authentication token
        final String newAuthenticationToken = generateAuthenticationToken();

        // set new authentication token in user and persist
        user.setAuthToken(newAuthenticationToken);
        try {
            // persist user with new token
            userManagement.updateUserData(user);
        } catch (NoSuchUserException | UserDataInsufficientException e) {
            log.error("cannot login user because of unexpected and fatal error: " + e.getMessage(), e);
            throw new InternalErrorException("cannot login user: " + e.getMessage(), e);
        }

        // generate JWT token that will be used by the client in the future
        String jwtToken = assembleJwtToken(user);

        // cache username and auth token
        loggedInUsersCache.put(user.getUsername(), user.getAuthToken());
        log.debug(String.format("stored new authentication token %s of user '%s' in cache", jwtToken, user.getUsername()));

        log.info(String.format("user '%s' was successfully logged in", user.getUsername()));
        return jwtToken;
    }

    /**
     * Creates JWT token that will be used by the client to authenticate himself.
     *
     * @param user user data must contain authToken and username
     * @return JWT Token string value
     * @throws InternalErrorException failed to create JWT Token caused by {@link JWTCreationException}
     * @author Daniel Mehlber
     */
    private String assembleJwtToken(final User user) throws InternalErrorException {
        String jwtToken;
        try {
            Algorithm algorithm = Algorithm.HMAC256(SECRET);
            jwtToken = JWT.create()
                    .withIssuer("com.connect.me")
                    .withClaim("username", user.getUsername())
                    .withClaim("authToken", user.getAuthToken())
                    .sign(algorithm);
        } catch (JWTCreationException e){
            log.error(String.format("cannot assemble JWT token because of fatal internal error: " + e.getMessage()));
            throw new InternalErrorException("cannot create new jwt token: "+ e.getMessage(), e);
        }

        log.debug(String.format("assembled new JWT token %s for user '%s'", jwtToken, user.getUsername()));
        return jwtToken;
    }

    /**
     * Generate a random authentication token
     * @return random authentication token
     * @author Daniel Mehlber
     */
    private String generateAuthenticationToken() {
        String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijk"
                +"lmnopqrstuvwxyz!@#$%&";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 32; i++)
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    /**
     * Logs out the user by removing his entry from cache and clearing the value of authToken in database
     *
     * @param user user that needs to be logged out
     * @throws UserDataInsufficientException data contained by the user object cannot be accepted by the system
     * @throws InternalErrorException an unexpected and fatal error occurred
     * @throws NoSuchUserException the user is not known to the system
     * @author Daniel Mehlber
     */
    public void logout(final User user) throws UserDataInsufficientException, InternalErrorException, NoSuchUserException {
        // remove user authentication token from cache
        loggedInUsersCache.remove(user.getUsername());
        log.debug(String.format("removed authentication token of user '%s' from cache", user.getUsername()));

        // remove user authentication token from database
        user.setAuthToken(null);
        try {
            userManagement.updateUserData(user);
        } catch (InternalErrorException | UserDataInsufficientException e) {
            log.error("cannot log-out user because of fatal internal error: " + e.getMessage());
            throw e;
        } catch (NoSuchUserException e) {
            log.warn("cannot logout user because he does not exist: " + e.getMessage());
            throw e;
        }

        log.info(String.format("user '%s' was successfully logged out", user.getUsername()));
    }

    /**
     * Check received JWT token before fetching user data from extracted username claim
     * @param jwt jwt token containing all information necessary for authentication
     * @return user data of authenticated user who is owner of the passed and valid JWT
     * @throws NoSuchUserException username claim in JWT is not valid, user does not exist
     * @throws InternalErrorException cannot connect to database and fetch user data
     * @throws FailedAuthenticationException wrong authentication token; JWT expired; JWT invalid
     * @author Daniel Mehlber
     */
    public User authenticateAndFetchUser(final String jwt) throws NoSuchUserException, InternalErrorException, FailedAuthenticationException {
        String username = authenticate(jwt);
        return userManagement.fetchUserByUsername(username);
    }

    /**
     * Check received JWT token and extract username out of it
     * @param jwt JWT token that needs to be processed
     * @throws FailedAuthenticationException user is not logged in or jwt is invalid
     * @return username stored in JWT token
     * @author Daniel Mehlber
     */
    public String authenticate(final String jwt) throws InternalErrorException, NoSuchUserException, FailedAuthenticationException {

        // extract username and authToken from JWT
        String username = "[unreadable]";
        String authToken;
        try {
            Algorithm algorithm = Algorithm.HMAC256(SECRET);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer("com.connect.me")
                    .build(); //Reusable verifier instance
            DecodedJWT decodedJWT = verifier.verify(jwt);
            username = decodedJWT.getClaim("username").asString();
            authToken = decodedJWT.getClaim("authToken").asString();
        } catch (JWTVerificationException e) {
            log.warn("authentication failed due to invalid JWT token: " + e.getMessage());
            throw new FailedAuthenticationException();
        } catch (RuntimeException e){
            log.error(String.format("cannot authenticate user '%s' due to internal error: %s", username, e.getMessage()));
            throw new InternalErrorException("cannot check jwt token due to an fatal internal error: " + e.getMessage(), e);
        }

        // check if user is authenticated
        if(!isAuthenticated(username, authToken)){
            log.warn(String.format("authentication failed: user '%s' is not logged in", HtmlUtils.htmlEscape(username)));
            throw new FailedAuthenticationException();
        }

        log.info(String.format("authentication successful: user '%s' is logged in", HtmlUtils.htmlEscape(username)));
        return username;
    }

    /**
     * Checks if user with username is logged in and can be authenticated with authToken.
     * @param username username of user that needs to be checked
     * @param claimAuthToken authentication token that should be associated to the username
     * @return true if the user is logged in and is authenticated
     * @throws NoSuchUserException user with username does not exist
     * @throws InternalErrorException an unexpected internal error occurred
     * @author Daniel Mehlber
     */
    private boolean isAuthenticated(final String username, final String claimAuthToken) throws NoSuchUserException, InternalErrorException {
        if(loggedInUsersCache.containsKey(username)) {
            // key is cached
            String actualAuthToken = loggedInUsersCache.get(username);
            return actualAuthToken.equals(claimAuthToken);
        } else {
            // key is not cached, must fetch it from database
            User user = userManagement.fetchUserByUsername(username);

            // store in cache for next time
            String actualAuthToken = user.getAuthToken();
            loggedInUsersCache.put(username, actualAuthToken);

            // compare auth tokens
            return claimAuthToken.equals(actualAuthToken);
        }
    }

    public void clearCache() {
        loggedInUsersCache.clear();
    }

}
