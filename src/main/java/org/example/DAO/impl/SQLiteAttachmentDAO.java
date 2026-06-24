package org.example.DAO.impl;

import org.example.DAO.AttachmentDAO;
import org.example.database.DBManager;
import org.example.models.Attachment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SQLiteAttachmentDAO implements AttachmentDAO {

    @Override
    public Attachment save(Attachment attachment) {
        String sql = "INSERT INTO attachments (message_id, file_name, file_path, file_size) VALUES (?, ?, ?, ?)";
        try (Connection connection = DBManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, attachment.getMessageId());
            statement.setString(2, attachment.getFileName());
            statement.setString(3, attachment.getFilePath());
            statement.setLong(4, attachment.getFileSize());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    attachment.setId(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return attachment;
    }

    @Override
    public List<Attachment> findByMessageId(long messageId) {
        List<Attachment> attachments = new ArrayList<>();
        String sql = "SELECT * FROM attachments WHERE message_id = ?";
        try (Connection connection = DBManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, messageId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    attachments.add(mapResultSetToAttachment(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return attachments;
    }

    @Override
    public void delete(long id) {
        String sql = "DELETE FROM attachments WHERE attachment_id = ?";
        try (Connection connection = DBManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Attachment mapResultSetToAttachment(ResultSet rs) throws SQLException {
        Attachment attachment = new Attachment();
        attachment.setId(rs.getLong("attachment_id"));
        attachment.setMessageId(rs.getLong("message_id"));
        attachment.setFileName(rs.getString("file_name"));
        attachment.setFilePath(rs.getString("file_path"));
        attachment.setFileSize(rs.getLong("file_size"));

        Timestamp timestamp = rs.getTimestamp("upload_time");
        if (timestamp != null) {
            attachment.setUploadTime(timestamp.toLocalDateTime());
        }
        return attachment;
    }
}