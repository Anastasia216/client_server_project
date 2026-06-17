package org.example.DAO;
import org.example.models.User;

import java.util.List;
import java.util.Optional;

public interface UserDAO {
    User save(User user);
    Optional<User> findById(long id);
    Optional<User> findByUsername(String username);
    List<User> findAll();
    void update(User user);
    void delete(long id);

}
