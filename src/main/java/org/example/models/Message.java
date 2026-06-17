package org.example.models;

import java.time.LocalDateTime;

public class Message {
    private long id;
    private long chatId;
    private long senderId;
    private String content;
    private LocalDateTime sentAt;
    private MessageStatus status;
    private MessageType type;
    private boolean isDeleted;

    public Message() {}

    public Message(long id, long chatId, long senderId, String content, LocalDateTime sentAt,
                   MessageStatus status, MessageType type, boolean isDeleted) {
        this.id = id;
        this.chatId = chatId;
        this.senderId = senderId;
        this.content = content;
        this.sentAt = sentAt;
        this.status = status;
        this.type = type;
        this.isDeleted = isDeleted;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getChatId() { return chatId; }
    public void setChatId(long chatId) { this.chatId = chatId; }

    public long getSenderId() { return senderId; }
    public void setSenderId(long senderId) { this.senderId = senderId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public String getStatus() { return status != null ? status.name() : null; }
    public void setStatus(String status) {
        this.status = status != null ? MessageStatus.valueOf(status) : null;
    }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }
}