package org.example.DAO.Impl;

import org.example.DAO.MessageDAO;
import org.example.database.DBManager;
import org.example.models.Message;
import org.example.models.MessageType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SQLiteMessageDAO implements MessageDAO {

    @Override
    public Message save(Message message) {
        String sql = "INSERT INTO messages (chat_id, sender_id, content, status, type, is_deleted) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = DBManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, message.getChatId());
            statement.setLong(2, message.getSenderId());
            statement.setString(3, message.getContent());
            statement.setString(4, message.getStatus());
            statement.setString(5, message.getType() != null ? message.getType().name() : MessageType.TEXT.name());
            statement.setBoolean(6, message.isDeleted());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    message.setId(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return message;
    }

    @Override
    public Optional<Message> findById(long id) {
        String sql = "SELECT * FROM messages WHERE message_id = ?";
        try (Connection connection = DBManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToMessage(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<Message> findByChatId(long chatId) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM messages WHERE chat_id = ? AND is_deleted = 0 ORDER BY sent_at ASC";
        try (Connection connection = DBManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, chatId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapResultSetToMessage(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    @Override
    public void update(Message message) {
        String sql = "UPDATE messages SET chat_id = ?, sender_id = ?, content = ?, status = ?, type = ?, is_deleted = ? WHERE message_id = ?";
        try (Connection connection = DBManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, message.getChatId());
            statement.setLong(2, message.getSenderId());
            statement.setString(3, message.getContent());
            statement.setString(4, message.getStatus());
            statement.setString(5, message.getType() != null ? message.getType().name() : MessageType.TEXT.name());
            statement.setBoolean(6, message.isDeleted());
            statement.setLong(7, message.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(long id) {
        String sql = "UPDATE messages SET is_deleted = 1 WHERE message_id = ?";
        try (Connection connection = DBManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Message mapResultSetToMessage(ResultSet rs) throws SQLException {
        Message message = new Message();
        message.setId(rs.getLong("id"));
        message.setChatId(rs.getLong("chat_id"));
        message.setSenderId(rs.getLong("sender_id"));
        message.setContent(rs.getString("content"));
        message.setStatus(rs.getString("status"));
        message.setDeleted(rs.getBoolean("is_deleted"));
        String typeStr = rs.getString("type");
        if (typeStr != null) {
            message.setType(MessageType.valueOf(typeStr));
        }
        Timestamp timestamp = rs.getTimestamp("sent_at");
        if (timestamp != null) {
            message.setSentAt(timestamp.toLocalDateTime());
        }
        return message;
    }
}