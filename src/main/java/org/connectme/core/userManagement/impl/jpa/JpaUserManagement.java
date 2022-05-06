package org.connectme.core.userManagement.impl.jpa;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.connectme.core.global.exceptions.InternalErrorException;
import org.connectme.core.userManagement.UserManagement;
import org.connectme.core.userManagement.entities.User;
import org.connectme.core.userManagement.exceptions.NoSuchUserException;
import org.connectme.core.userManagement.exceptions.UserDataInsufficientException;
import org.connectme.core.userManagement.exceptions.UsernameAlreadyTakenException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@SuppressWarnings("unused")
@Component("JpaUserManagement")
@Primary
public class JpaUserManagement implements UserManagement {

    @SuppressWarnings("unused")
    @Autowired
    UserRepository userRepository;

    private final Logger log = LogManager.getLogger(UserManagement.class);

    @Override
    public boolean isUsernameAvailable(final String username) throws RuntimeException, InternalErrorException {
        log.debug("checking if username is available...");
        try {
            boolean isAvailable = !userRepository.existsById(username);
            log.debug("username availability check: username is " + (isAvailable ? "" : "not") + " available");
            return isAvailable;
        } catch (RuntimeException e) {
            log.error("cannot check if username is available: a runtime error has occurred: " + e.getMessage());
            throw new InternalErrorException("cannot check if username is available", e);
        }
    }

    @Override
    public User fetchUserByUsername(final String username) throws RuntimeException, InternalErrorException, NoSuchUserException {
        log.debug("fetching user by username...");
        try {
            return userRepository.findById(username).orElseThrow(() -> new NoSuchUserException(username));
        } catch (RuntimeException e) {
            log.error("cannot fetch user by username: an unexpected runtime error occurred: " + e.getMessage());
            throw new InternalErrorException("cannot fetch user by id", e);
        }
    }

    @Override
    public void createNewUser(final User userdata) throws RuntimeException, InternalErrorException, UsernameAlreadyTakenException, UserDataInsufficientException {
        log.debug("creating new user from userdata...");
        try {
            if (userRepository.existsById(userdata.getUsername())) {
                log.warn(String.format("cannot create user with username '%s' because it is already taken",
                        HtmlUtils.htmlEscape(userdata.getUsername())));
                throw new UsernameAlreadyTakenException(userdata.getUsername());
            } else {
                userRepository.save(userdata);
            }
        } catch (DataIntegrityViolationException e) {
            // if database integrity rules or checks (data length, regex, etc.) are not met, this runtime exception is thrown
            log.warn(String.format("cannot create new user '%s' because database does not accept passed user data: %s",
                    HtmlUtils.htmlEscape(userdata.getUsername()), e.getMessage()));
            throw new UserDataInsufficientException(e);
        } catch (RuntimeException e) {
            log.error(String.format("cannot create new user '%s': an unexpected runtime error occurred: %s"),
                    HtmlUtils.htmlEscape(userdata.getUsername()), e.getMessage());
            throw new InternalErrorException("cannot create new user", e);
        }
        log.debug(String.format("successfully created new user '%s' and persisted it in database",
                HtmlUtils.htmlEscape(userdata.getUsername())));
    }

    @Override
    public void updateUserData(final User userdata) throws RuntimeException, InternalErrorException, NoSuchUserException, UserDataInsufficientException {
        log.debug("updating user data of existing user...");
        try {
            if(!userRepository.existsById(userdata.getUsername())) {
                log.warn("cannot update user data because user does not exist");
                throw new NoSuchUserException(userdata.getUsername());
            } else {
                userRepository.save(userdata);
            }
        } catch (DataIntegrityViolationException e) {
          // if database integrity rules or checks (data length, regex, etc.) are not met, this runtime exception is thrown
          log.warn("cannot update user data because database does not accept passed user data: " + e.getMessage());
          throw new UserDataInsufficientException(e);
        } catch (RuntimeException e) {
            log.error("cannot update user data: an unexpected runtime error occurred: " + e.getMessage());
            throw new InternalErrorException("cannot update user data", e);
        }
        log.debug("successfully updated user data of existing user");
    }

    @Override
    public void deleteUser(final String username) throws RuntimeException, InternalErrorException, NoSuchUserException {
        log.debug("deleting user data of user with passed username");
        try {
            if(!userRepository.existsById(username)) {
                log.warn("cannot delete user data because user does not exist");
                throw new NoSuchUserException(username);
            } else {
                userRepository.deleteById(username);
            }
        } catch (RuntimeException e) {
            log.error("cannot delete user: an unexpected runtime error occurred: " + e.getMessage());
            throw new InternalErrorException("cannot delete user", e);
        }
        log.debug("successfully deleted user");
    }
}