package org.example.DAO.impl;

import org.example.DAO.UserDAO;
import org.example.database.DBManager;
import org.example.models.User;

import java.sql.*;
        import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SQLiteUserDAO implements UserDAO {

    @Override
    public User save(User user) {
        String sql = """
            INSERT INTO users (username, phone, password_hash, role, status, is_blocked) 
            VALUES (?, ?, ?, 
                CASE WHEN (SELECT COUNT(*) FROM users) = 0 THEN 'ADMIN' ELSE 'USER' END, 
            ?, ?)
        """;

        try (Connection connection = DBManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPhone());
            statement.setString(3, user.getPasswordHash());
            statement.setString(4, user.getStatus());
            statement.setBoolean(5, user.isBlocked());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setUser_id(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return user;
    }

    @Override
    public Optional<User> findById(long user_id) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection connection = DBManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, user_id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSetToUser(resultSet));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection connection = DBManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSetToUser(resultSet));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        String sql = "SELECT * FROM users WHERE phone = ?";
        try (Connection connection = DBManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, phone);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSetToUser(resultSet));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection connection = DBManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                users.add(mapResultSetToUser(resultSet));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    @Override
    public void update(User user) {
        String sql = "UPDATE users SET username = ?, phone = ?, password_hash = ?, role = ?, status = ?, is_blocked = ? WHERE user_id = ?";
        try (Connection connection = DBManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPhone());
            statement.setString(3, user.getPasswordHash());
            statement.setString(4, user.getRole());
            statement.setString(5, user.getStatus());
            statement.setBoolean(6, user.isBlocked());
            statement.setLong(7, user.getUser_id());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(long user_id) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection connection = DBManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, user_id);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private User mapResultSetToUser(ResultSet resultSet) throws SQLException {
        return new User(
                resultSet.getInt("user_id"),
                resultSet.getString("username"),
                resultSet.getString("phone"),
                resultSet.getString("password_hash"),
                resultSet.getString("role"),
                resultSet.getString("status"),
                resultSet.getBoolean("is_blocked")
        );
    }

    public boolean isUsernameUnique(String username, long currentUserId) {
        String sql = "SELECT 1 FROM users WHERE username = ? AND user_id != ?";
        try (Connection connection = DBManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setLong(2, currentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return !resultSet.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isPhoneUnique(String phone, long currentUserId) {
        String sql = "SELECT 1 FROM users WHERE phone = ? AND user_id != ?";
        try (Connection connection = DBManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, phone);
            statement.setLong(2, currentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return !resultSet.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public String deleteUserAccountFully(long userId) {
        String checkSysRoleSql = "SELECT role FROM users WHERE user_id = ?";
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(checkSysRoleSql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && "ADMIN".equalsIgnoreCase(rs.getString("role"))) {
                    return "ERROR:SYSTEM_ADMIN_CANNOT_BE_DELETED";
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "ERROR:DATABASE_ERROR";
        }

        String checkGroupAdminsSql = """
            SELECT cm.chat_id, c.chat_name 
            FROM chat_members cm
            JOIN chats c ON cm.chat_id = c.chat_id
            WHERE cm.user_id = ? AND cm.role = 'ADMIN' AND c.type = 'GROUP'
              AND (SELECT COUNT(*) FROM chat_members WHERE chat_id = cm.chat_id AND role = 'ADMIN') <= 1
        """;
        try (Connection conn = DBManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(checkGroupAdminsSql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String groupName = rs.getString("chat_name");
                    return "ERROR:SOLE_ADMIN_IN_GROUP;" + groupName;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "ERROR:DATABASE_ERROR";
        }

        try (Connection conn = DBManager.getConnection()) {
            conn.setAutoCommit(false);

            try {
                String deleteMessagesSql = "DELETE FROM messages WHERE sender_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteMessagesSql)) {
                    stmt.setLong(1, userId);
                    stmt.executeUpdate();
                }

                String findPrivateChatsSql = """
                    SELECT chat_id FROM chats WHERE type = 'PRIVATE' AND chat_id IN 
                    (SELECT chat_id FROM chat_members WHERE user_id = ?)
                """;
                List<Long> privateChatIds = new ArrayList<>();
                try (PreparedStatement stmt = conn.prepareStatement(findPrivateChatsSql)) {
                    stmt.setLong(1, userId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) privateChatIds.add(rs.getLong("chat_id"));
                    }
                }

                for (long chatId : privateChatIds) {
                    try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM chat_members WHERE chat_id = ?")) {
                        stmt.setLong(1, chatId);
                        stmt.executeUpdate();
                    }
                    try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM messages WHERE chat_id = ?")) {
                        stmt.setLong(1, chatId);
                        stmt.executeUpdate();
                    }
                    try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM chats WHERE chat_id = ?")) {
                        stmt.setLong(1, chatId);
                        stmt.executeUpdate();
                    }
                }

                String deleteMembershipsSql = "DELETE FROM chat_members WHERE user_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteMembershipsSql)) {
                    stmt.setLong(1, userId);
                    stmt.executeUpdate();
                }

                String deleteUserSql = "DELETE FROM users WHERE user_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteUserSql)) {
                    stmt.setLong(1, userId);
                    stmt.executeUpdate();
                }

                conn.commit();
                return "SUCCESS:ACCOUNT_DELETED";

            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                return "ERROR:TRANSACTION_FAILED";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "ERROR:DATABASE_ERROR";
        }
    }
}