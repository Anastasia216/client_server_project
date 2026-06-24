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

    @Override
    public void renameChat(long chatId, String newName) {
        String sql = "UPDATE chats SET chat_name = ? WHERE chat_id = ?";
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newName);
            stmt.setLong(2, chatId);
            stmt.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public boolean isAdmin(long userId, long chatId) {
        String sql = "SELECT role FROM chat_members WHERE chat_id = ? AND user_id = ?";
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, chatId);
            stmt.setLong(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && "ADMIN".equalsIgnoreCase(rs.getString("role"));
            }
        } catch (Exception e) { return false; }
    }
    @Override
    public boolean isMember(long chatId, long userId) {
        String sql = "SELECT 1 FROM chat_members WHERE chat_id = ? AND user_id = ?";
        try (Connection conn = DBManager.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, chatId);
            stmt.setLong(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    @Override
    public void updateMemberRole(long chatId, long userId, String newRole) {
        String sql = "UPDATE chat_members SET role = ? WHERE chat_id = ? AND user_id = ?";
        try (Connection conn = org.example.database.DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newRole);
            stmt.setLong(2, chatId);
            stmt.setLong(3, userId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeMember(long chatId, long userId) {
        String sql = "DELETE FROM chat_members WHERE chat_id = ? AND user_id = ?";
        try (Connection conn = org.example.database.DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, chatId);
            stmt.setLong(2, userId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public String getGroupMembersData(long chatId) {
        String sql = "SELECT u.user_id, u.username, cm.role FROM chat_members cm " +
                "JOIN users u ON cm.user_id = u.user_id WHERE cm.chat_id = ? ORDER BY cm.role ASC, u.username ASC";
        StringBuilder sb = new StringBuilder();
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, chatId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sb.append(rs.getInt("user_id")).append(":::")
                            .append(rs.getString("username")).append(":::")
                            .append(rs.getString("role")).append("|||");
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return sb.length() > 0 ? sb.substring(0, sb.length() - 3) : "";
    }
    @Override
    public boolean isSoleAdmin(long chatId, long userId) {
        String sql = "SELECT role, (SELECT COUNT(*) FROM chat_members WHERE chat_id = ? AND role = 'ADMIN') as admin_count " +
                "FROM chat_members WHERE chat_id = ? AND user_id = ?";
        try (Connection conn = DBManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, chatId);
            stmt.setLong(2, chatId);
            stmt.setLong(3, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return "ADMIN".equalsIgnoreCase(rs.getString("role")) && rs.getInt("admin_count") <= 1;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    @Override
    public boolean privateChatExists(long user1Id, long user2Id) {
        String sql = """
        SELECT c.chat_id FROM chats c
        JOIN chat_members cm1 ON c.chat_id = cm1.chat_id
        JOIN chat_members cm2 ON c.chat_id = cm2.chat_id
        WHERE c.type = 'PRIVATE' AND cm1.user_id = ? AND cm2.user_id = ?
    """;
        try (Connection conn = DBManager.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, user1Id); stmt.setLong(2, user2Id);
            try (ResultSet rs = stmt.executeQuery()) { return rs.next(); }
        } catch (Exception e) { e.printStackTrace(); return false; }
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
    @Override
    public String getUserChatsData(long userId) {
        String sql = """
        SELECT chats.chat_id, chats.chat_name, chats.type, chat_members.role,
               (SELECT COUNT(*) FROM messages 
                WHERE messages.chat_id = chats.chat_id 
                  AND messages.sender_id != ? 
                  AND (messages.status != 'READ' OR messages.status IS NULL)) as unread_count
        FROM chats
        JOIN chat_members ON chats.chat_id = chat_members.chat_id
        WHERE chat_members.user_id = ?
    """;

        StringBuilder sb = new StringBuilder();
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int chatId = rs.getInt("chat_id");
                    String chatName = rs.getString("chat_name");
                    String chatType = rs.getString("type");
                    String userRole = rs.getString("role");
                    int unreadCount = rs.getInt("unread_count");

                    if (userRole == null || "OWNER".equalsIgnoreCase(userRole)) userRole = "ADMIN";

                    if ("PRIVATE".equalsIgnoreCase(chatType)) {
                        String privateNameSql = "SELECT u.username, u.status FROM chat_members cm " +
                                "JOIN users u ON cm.user_id = u.user_id " +
                                "WHERE cm.chat_id = ? AND cm.user_id != ?";
                        try (PreparedStatement pStmt = conn.prepareStatement(privateNameSql)) {
                            pStmt.setInt(1, chatId);
                            pStmt.setLong(2, userId);
                            try (ResultSet pRs = pStmt.executeQuery()) {
                                if (pRs.next()) {
                                    String status = pRs.getString("status");
                                    chatName = pRs.getString("username") + " | " +
                                            (status == null || status.isEmpty() ? "OFFLINE" : status.toUpperCase());
                                }
                            }
                        }
                    }
                    sb.append(chatName).append(":::")
                            .append(chatType).append(":::")
                            .append(userRole).append(" (ID: ").append(chatId).append(")")
                            .append(":::").append(unreadCount).append(";");
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return sb.toString();
    }
    @Override
    public String getChatHistory(long chatId) {
        String sql = "SELECT u.username, m.content FROM messages m JOIN users u ON m.sender_id = u.user_id WHERE m.chat_id = ? ORDER BY m.message_id ASC";
        StringBuilder sb = new StringBuilder();
        try (Connection conn = DBManager.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, chatId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sb.append(rs.getString("username")).append(":::")
                            .append(rs.getString("content")).append("\n");
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return sb.toString();
    }
}