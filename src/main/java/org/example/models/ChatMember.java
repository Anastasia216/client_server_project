package org.example.models;

public class ChatMember {

    private long chatId;
    private long userId;
    private ChatRole role;

    public ChatMember() {
    }

    public ChatMember(long chatId, long userId, ChatRole role) {
        this.chatId = chatId;
        this.userId = userId;
        this.role = role;
    }

    public long getChatId() {
        return chatId;
    }

    public void setChatId(long chatId) {
        this.chatId = chatId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public ChatRole getRole() {
        return role;
    }

    public void setRole(ChatRole role) {
        this.role = role;
    }
}