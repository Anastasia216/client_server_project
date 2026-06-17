package org.example.service;

import org.example.DAO.ChatDAO;
import org.example.DAO.ChatMemberDAO;
import org.example.models.Chat;
import org.example.models.ChatMember;
import org.example.models.ChatRole;
import org.example.models.ChatType;

import java.util.ArrayList;
import java.util.List;

public class ChatService {
    private final ChatDAO chatDAO;
    private final ChatMemberDAO chatMemberDAO;

    public ChatService(ChatDAO chatDAO, ChatMemberDAO chatMemberDAO) {
        this.chatDAO = chatDAO;
        this.chatMemberDAO = chatMemberDAO;
    }

    public Chat createChat(String name, ChatType type, long creatorId) {
        Chat chat = new Chat();
        chat.setName(name);
        chat.setType(type);

        Chat savedChat = chatDAO.save(chat);

        ChatMember creator = new ChatMember(savedChat.getId(), creatorId, ChatRole.ADMIN);
        chatMemberDAO.addMember(creator);

        return savedChat;
    }

    public boolean addMemberToChat(long chatId, long userId, ChatRole role) {
        if (chatMemberDAO.findMember(chatId, userId).isPresent()) {
            return false;
        }

        ChatMember newMember = new ChatMember(chatId, userId, role);
        chatMemberDAO.addMember(newMember);
        return true;
    }

    public void removeMemberFromChat(long chatId, long userId) {
        chatMemberDAO.removeMember(chatId, userId);
    }

    public List<Chat> getChatsForUser(long userId) {
        List<ChatMember> memberships = chatMemberDAO.findByUserId(userId);
        List<Chat> userChats = new ArrayList<>();

        for (ChatMember member : memberships) {
            chatDAO.findById(member.getChatId()).ifPresent(userChats::add);
        }
        return userChats;
    }

    public List<ChatMember> getChatMembers(long chatId) {
        return chatMemberDAO.findByChatId(chatId);
    }

    public void deleteChat(long chatId) {
        chatDAO.delete(chatId);
    }
}