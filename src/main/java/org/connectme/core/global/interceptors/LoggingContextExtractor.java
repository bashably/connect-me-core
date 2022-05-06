package org.connectme.core.global.interceptors;


import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;


/**
 * This filter intercepts all requests and reads information important for logging such as the session-id. It also
 * generates values like the request-uuid and appends both to the {@link MDC} for Log4j to read from.
 *
 * @author Daniel Mehlber
 */
@Component
public class LoggingContextExtractor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // read information out of request that is needed for logging
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String sessionID = httpRequest.getSession().getId();
        String requestID = String.valueOf(UUID.randomUUID());

        // put values into MDC (log4j reads from the MDC)
        MDC.put("sessionId", sessionID);
        MDC.put("requestId", requestID);
        return true;
    }


    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        MDC.clear();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // clear values from MDC after request has been finished
        MDC.clear();
    }
}
