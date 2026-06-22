package org.example.service;

import org.example.DAO.UserDAO;
import org.example.models.User;
import org.example.models.UserRole;
import org.example.models.UserStatus;
import org.example.util.PasswordHasher;
import java.util.Optional;

public class AuthService {
    private final UserDAO userDAO;

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public Optional<User> registerFromRaw(String rawData) {
        String[] parts = rawData.split(";");
        if (parts.length < 3) return Optional.empty();

        String username = parts[0];
        String phone = parts[1];
        String password = parts[2];

        if (userDAO.findByUsername(username).isPresent() || userDAO.findByPhone(phone).isPresent()) {
            return Optional.empty();
        }

        User user = new User(0, username, phone, PasswordHasher.hash(password),
                UserRole.USER.name(), UserStatus.ONLINE.name(), false);

        user = userDAO.save(user);
        return Optional.of(user);
    }

    public Optional<User> loginFromRaw(String rawData) {
        String[] parts = rawData.split(";");
        if (parts.length < 2) return Optional.empty();

        String username = parts[0];
        String password = parts[1];

        Optional<User> userOpt = userDAO.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (!user.isBlocked() && PasswordHasher.check(password, user.getPasswordHash())) {
                user.setStatus(UserStatus.ONLINE.name());
                userDAO.update(user);
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }
    public void updateStatus(long userId, String status){
        userDAO.findById(userId).ifPresent(user -> {
            user.setStatus(status);
            userDAO.update(user);
        });
    }
    public void BlockUser(long userId){
        userDAO.findById(userId).ifPresent(user -> {
            user.setBlocked(true);
            user.setStatus(org.example.models.UserStatus.OFFLINE.name());
            userDAO.update(user);
        });
    }
}