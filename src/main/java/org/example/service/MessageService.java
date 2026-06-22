package org.example.service;

import org.example.DAO.MessageDAO;
import org.example.models.Message;
import org.example.models.MessageStatus;
import org.example.models.MessageType;

import java.util.List;
import java.util.stream.Collectors;

public class MessageService {
    private final MessageDAO messageDAO;

    public MessageService(MessageDAO messageDAO) {
        this.messageDAO = messageDAO;
    }
    // "chatId, message-text"
    public void sendMessageFromRaw(int senderId, String rawData) {
        String[] parts = rawData.split(";", 2); // Розбиваємо тільки на 2 частини
        if (parts.length < 2) return;

        int chatId = Integer.parseInt(parts[0]);
        String content = parts[1];

        if (!content.strip().isEmpty()) {
            Message message = new Message();
            message.setChatId(chatId);
            message.setSenderId(senderId);
            message.setContent(content.trim());
            message.setStatus(MessageStatus.SENT.name());
            message.setType(MessageType.TEXT);
            message.setDeleted(false);

            messageDAO.save(message);
        }
    }
    public void saveMessage(long chatId, long senderId, String text){
        if (text == null || text.trim().isEmpty()){
            return;
        }
        Message message = new Message();
        message.setChatId(chatId);
        message.setSenderId(senderId);
        message.setContent(text.trim());
        message.setSentAt(java.time.LocalDateTime.now());
        message.setStatus(MessageStatus.SENT.name());
        message.setType(MessageType.TEXT);
        message.setDeleted(false);

        messageDAO.save(message);
    }
    public String getChatHistoryRaw(long chatId) {
        List<Message> history = messageDAO.findByChatId(chatId);
        return history.stream().map(Message::getContent).collect(Collectors.joining("\n"));
    }

    public void deleteMessage(long messageId) {
        messageDAO.delete(messageId);
    }
}