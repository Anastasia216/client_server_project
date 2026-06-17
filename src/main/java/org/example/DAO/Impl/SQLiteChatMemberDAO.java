package org.example.DAO.Impl;

import org.example.DAO.ChatMemberDAO;
import org.example.database.DBManager;
import org.example.models.ChatMember;
import org.example.models.ChatRole;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SQLiteChatMemberDAO implements ChatMemberDAO {

    @Override
    public void addMember(ChatMember member) {
        String sql = "INSERT INTO chat_members (chat_id, user_id, role) VALUES (?, ?, ?)";
        try (Connection connection = DBManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, member.getChatId());
            statement.setLong(2, member.getUserId());
            statement.setString(3, member.getRole() != null ? member.getRole().name() : null);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeMember(long chatId, long userId) {
        String sql = "DELETE FROM chat_members WHERE chat_id = ? AND user_id = ?";
        try (Connection connection = DBManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, chatId);
            statement.setLong(2, userId);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<ChatMember> findByChatId(long chatId) {
        List<ChatMember> members = new ArrayList<>();
        String sql = "SELECT * FROM chat_members WHERE chat_id = ?";
        try (Connection connection = DBManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, chatId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    members.add(mapResultSetToMember(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return members;
    }

    @Override
    public List<ChatMember> findByUserId(long userId) {
        List<ChatMember> members = new ArrayList<>();
        String sql = "SELECT * FROM chat_members WHERE user_id = ?";
        try (Connection connection = DBManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    members.add(mapResultSetToMember(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return members;
    }

    @Override
    public Optional<ChatMember> findMember(long chatId, long userId) {
        String sql = "SELECT * FROM chat_members WHERE chat_id = ? AND user_id = ?";
        try (Connection connection = DBManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, chatId);
            statement.setLong(2, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToMember(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    private ChatMember mapResultSetToMember(ResultSet rs) throws SQLException {
        ChatMember member = new ChatMember();
        member.setChatId(rs.getLong("chat_id"));
        member.setUserId(rs.getLong("user_id"));
        String roleStr = rs.getString("role");
        if (roleStr != null) {
            member.setRole(ChatRole.valueOf(roleStr));
        }
        return member;
    }
}