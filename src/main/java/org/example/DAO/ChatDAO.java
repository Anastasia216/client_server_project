package org.example.DAO;
import org.example.models.Chat;

import java.util.List;
import java.util.Optional;

public interface ChatDAO {
    Chat save(Chat chat);
    Optional<Chat> findById(long id);
    List<Chat> findAll();
    void delete(long id);
}
