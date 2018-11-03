package com.borunovv.jogging.users.service;

import com.borunovv.core.hibernate.exception.DuplicateEntryException;
import com.borunovv.core.service.AbstractService;
import com.borunovv.core.util.Assert;
import com.borunovv.core.util.CryptUtils;
import com.borunovv.jogging.users.dao.UserDao;
import com.borunovv.jogging.users.model.Rights;
import com.borunovv.jogging.users.model.User;
import com.borunovv.jogging.users.util.CredentialsValidator;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

@Service
public class UserService extends AbstractService {

    public User registerNewUser(String login, String password) {
        CredentialsValidator.validateCredentials(login, password);
        User user = new User();
        user.setLogin(login);
        user.setPassHash(getPasswordHash(password));
        user.setRights(Rights.USER);
        try {
            dao.save(user);
        } catch (DuplicateEntryException e) {
            throw new RuntimeException("USER already exists.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create new user.", e);
        }
        return user;
    }

    // Return new session.
    public String loginUser(String login, String pass) {
        User user = dao.tryFind(login, getPasswordHash(pass));
        Assert.notNull(user, "Not found user with given login and password.");

        return sessionService.startNewSession(user);
    }

    public User ensureUser(String login) {
        User user = dao.tryFind(login);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return user;
    }

    public User ensureUser(long userId) {
        User user = dao.tryFind(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return user;
    }


    public void updatePassword(User userToUpdate, String newPassword) {
        CredentialsValidator.validateCredentials(userToUpdate.getLogin(), newPassword);
        userToUpdate.setPassHash(getPasswordHash(newPassword));
        dao.save(userToUpdate);
    }

    public void updateRights(User userToUpdate, Rights newRights) {
        if (newRights != null && userToUpdate.getRights() != newRights) {
            userToUpdate.setRights(newRights);
            dao.save(userToUpdate);
        }
    }

    public void delete(User userToDelete) {
        sessionService.clearSessionsFor(userToDelete);
        // TODO: remove timings!
        dao.delete(userToDelete);
    }

    public List<User> findAll(long offset, long count, List<Rights> rightsList) {
        return dao.findAll(offset, count, rightsList);
    }


    private String getPasswordHash(String password) {
        return CryptUtils.md5(password);
    }

    @Inject
    private UserDao dao;
    @Inject
    private SessionService sessionService;
}
