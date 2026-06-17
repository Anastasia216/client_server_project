package org.example.service;

import org.example.DAO.LogDAO;
import org.example.DAO.UserDAO;
import org.example.DAO.ChatDAO;
import org.example.DAO.MessageDAO; // Не забудь додати імпорт
import org.example.models.Log;
import org.example.models.UserStatus;

import java.util.List;

public class LogAndStatsService {
    private final LogDAO logDAO;
    private final UserDAO userDAO;
    private final ChatDAO chatDAO;
    private final MessageDAO messageDAO; // Додано

    public LogAndStatsService(LogDAO logDAO, UserDAO userDAO, ChatDAO chatDAO, MessageDAO messageDAO) {
        this.logDAO = logDAO;
        this.userDAO = userDAO;
        this.chatDAO = chatDAO;
        this.messageDAO = messageDAO; // Додано
    }

    public void logSystemEvent(String level, String message) {
        Log log = new Log();
        log.setLevel(level);
        log.setMessage(message);
        logDAO.save(log);
    }

    public List<Log> getAllLogs() {
        return logDAO.findAll();
    }

    public long getOnlineUsersCount() {
        return userDAO.findAll().stream()
                .filter(user -> UserStatus.ONLINE.name().equals(user.getStatus()))
                .count();
    }

    public int getTotalUsersCount() {
        return userDAO.findAll().size();
    }

    public int getTotalChatsCount() {
        return chatDAO.findAll().size();
    }

    public int getTotalMessagesCount(long chatId) {
        return messageDAO.findByChatId(chatId).size();
    }
}