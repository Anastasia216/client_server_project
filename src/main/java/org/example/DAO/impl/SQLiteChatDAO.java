package org.example.DAO.impl;
import org.example.DAO.ChatDAO;
import org.example.database.DBManager;
import org.example.models.Chat;
import org.example.models.ChatType;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SQLiteChatDAO implements ChatDAO {
    @Override
    public Chat save(Chat chat) {
        String sql = "INSERT INTO chats (chat_name, type) VALUES (?, ?)";
        try (Connection connection = DBManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, chat.getName());
            statement.setString(2, chat.getType() != null ? chat.getType().name() : null);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    chat.setId(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return chat;
    }

    @Override
    public Optional<Chat> findById(long id) {
        String sql = "SELECT * FROM chats WHERE chat_id = ?";
        try (Connection connection = DBManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToChat(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<Chat> findAll() {
        List<Chat> chats = new ArrayList<>();
        String sql = "SELECT * FROM chats";
        try (Connection connection = DBManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                chats.add(mapResultSetToChat(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return chats;
    }

    @Override
    public void delete(long id) {
        String sql = "DELETE FROM chats WHERE chat_id = ?";
        try (Connection connection = DBManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Chat mapResultSetToChat(ResultSet rs) throws SQLException {
        Chat chat = new Chat();
        chat.setId(rs.getLong("chat_id"));
        chat.setName(rs.getString("chat_name"));
        String typeStr = rs.getString("type");
        if (typeStr != null) {
            chat.setType(ChatType.valueOf(typeStr));
        }
        Timestamp timestamp = rs.getTimestamp("created_at");
        if (timestamp != null) {
            chat.setCreatedAt(timestamp.toLocalDateTime());
        }
        return chat;
    }
}