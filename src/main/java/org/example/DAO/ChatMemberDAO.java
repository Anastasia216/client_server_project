package org.example.DAO;
import org.example.models.ChatMember;

import java.util.List;
import java.util.Optional;

public interface ChatMemberDAO {
    void addMember(ChatMember member);
    void removeMember(long chatId, long userId);
    List<ChatMember> findByChatId(long chatId);
    List<ChatMember> findByUserId(long userId);
    Optional<ChatMember> findMember(long chatId, long userId);

}