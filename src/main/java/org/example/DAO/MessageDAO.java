package org.example.DAO;
import org.example.models.Message;

import java.util.List;
import java.util.Optional;

public interface MessageDAO {
    Message save(Message message);
    Optional<Message> findById(long id);
    List<Message> findByChatId(long chatId);
    void update(Message message);
    void delete(long id);
    long countAll();
    long getLastMessageId();
    void markMessagesAsRead(long chatId, long userId);
}
