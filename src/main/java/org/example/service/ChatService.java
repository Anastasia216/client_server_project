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
    public void renameChat(int chatId, String newName) {
        chatDAO.renameChat(chatId, newName);
    }
    public boolean isAdminOfChat(long userId, int chatId) {
        return chatDAO.isAdmin(userId, chatId);
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
    public List<Long> getChatMemberIds(long chatId){
        return chatMemberDAO.findByChatId(chatId).stream().map(ChatMember::getUserId).collect(java.util.stream.Collectors.toList());
    }
    public void deleteChat(long chatId) {
        chatDAO.delete(chatId);
    }
    public Chat createGroup(String chatName, long creatorId) {
        return createChat(chatName, org.example.models.ChatType.GROUP, creatorId);
    }
    public boolean addUserToChat(long chatId, long userId){
        return addMemberToChat(chatId, userId, org.example.models.ChatRole.USER);
    }
    public void promoteUserToAdmin(int chatId, int targetUserId) {
        chatDAO.updateMemberRole(chatId, targetUserId, "ADMIN");
    }

    public void removeUserFromGroup(int chatId, int targetUserId) {
        chatDAO.removeMember(chatId, targetUserId);
    }
    public String getGroupMembersData(int chatId) {
        return chatDAO.getGroupMembersData(chatId);
    }
    public void leaveChat(int chatId, long userId) {
        chatDAO.removeMember(chatId, userId);
    }
    public boolean isSoleAdmin(int chatId, long userId) {
        return chatDAO.isSoleAdmin(chatId, userId);
    }
    public boolean isMember(int chatId, long userId) {
        return chatDAO.isMember(chatId, userId);
    }
    public boolean privateChatExists(long user1Id, long user2Id) {
        return chatDAO.privateChatExists(user1Id, user2Id);
    }
    public String getUserChats(long userId) {
        return chatDAO.getUserChatsData(userId);
    }
    public String getChatHistory(int chatId) {
        return chatDAO.getChatHistory(chatId);
    }
}