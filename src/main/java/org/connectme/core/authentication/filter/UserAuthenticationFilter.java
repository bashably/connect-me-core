package org.connectme.core.authentication.filter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.connectme.core.WebMvcConfiguration;
import org.connectme.core.authentication.beans.UserAuthenticationBean;
import org.connectme.core.global.exceptions.InternalErrorException;
import org.connectme.core.userManagement.entities.User;
import org.connectme.core.userManagement.exceptions.FailedAuthenticationException;
import org.connectme.core.userManagement.exceptions.NoSuchUserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This filter tries to authenticate the user using his passed JWT token and only lets through authenticated users.
 * Filter URL Patterns are set in {@link WebMvcConfiguration#addAuthenticationFilter()}.
 * @author Daniel Mehlber
 */
@Component
public class UserAuthenticationFilter implements Filter {

    public static final String HTTP_ATTR_USER = "user";

    private final Logger log = LogManager.getLogger(UserAuthenticationFilter.class);

    @Autowired
    private UserAuthenticationBean authenticationBean;

    /**
     * <h1>Actions</h1>
     * <p>Extracts the JWT contained in the "authentication" header of the HTTP request and performs the authentication.
     *      <ul>
     *          <li>If the authentication was successful, the user object will be placed in the request attribute "user" and the request can
     *          continue its journey.</li>
     *          <li>In any other case the request will bounce off and an error code will be returned to the user.</li>
     *      </ul>
     * </p>
     * <h1>Protected URL patterns</h1>
     * <p>
     *      The protected URL patterns can be set in {@link WebMvcConfiguration#addAuthenticationFilter()}, they are not set
     *      in this method. This method will be invoked for every request matching the url patterns defined in the filter
     *      installation.
     * </p>
     * <h1>HTTP Actions</h1>
     * <p>
     *     <ul>
     *         <li>The header attribute "authentication" is read and must contain a valid JWT</li>
     *         <li>The request attribute "user" will be written when the authentication has been completed successfully.
     *         It contains the authenticated user data fetched from database</li>
     *     </ul>
     * </p>
     * @param req  The request to process
     * @param resp The response associated with the request
     * @param chain    Provides access to the next filter in the chain for this
     *                 filter to pass the request and response to for further
     *                 processing
     *
     * @throws IOException io error
     * @throws ServletException some unexpected runtime error occurred
     * @author Daniel Mehlber
     * @see UserAuthenticationBean#authenticate(String)
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) resp;
        HttpServletRequest request = (HttpServletRequest) req;
        String jwt = request.getHeader("authentication");

        // check if jwt token is provided
        if(jwt == null) {
            log.warn("access was requested, but no jwt token was provided. Request was rejected.");
            // no jwt was provided. cannot continue authentication
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("no jwt provided");
            return;
        }

        // perform authentication
        User authenticatedUser;
        try {
            authenticatedUser = authenticationBean.authenticateAndFetchUser(jwt);
        } catch (NoSuchUserException | FailedAuthenticationException e) {
            log.warn(String.format("user authentication with jwt token [%s] failed: " + e.getMessage()));
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        } catch (InternalErrorException e) {
            log.fatal(String.format("cannot complete user authentication due to an internal error using JWT Token [%s] - %s", jwt, e.getMessage()), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        // place authenticated user in request and continue
        request.setAttribute(HTTP_ATTR_USER, authenticatedUser);
        chain.doFilter(request, response);
    }


}
