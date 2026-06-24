package org.example.DAO;
import org.example.models.Chat;

import java.util.List;
import java.util.Optional;

public interface ChatDAO {
    Chat save(Chat chat);
    Optional<Chat> findById(long id);
    List<Chat> findAll();
    void delete(long id);
    void renameChat(long chatId, String newName);
    boolean isAdmin(long userId, long chatId);
    boolean isMember(long chatId, long userId);
    void updateMemberRole(long chatId, long userId, String newRole);
    void removeMember(long chatId, long userId);
    String getGroupMembersData(long chatId);
    boolean isSoleAdmin(long chatId, long userId);
    boolean privateChatExists(long user1Id, long user2Id);
    String getUserChatsData(long userId);
    String getChatHistory(long chatId);
}
