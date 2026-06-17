package org.example.DAO.Impl;

import org.example.DAO.LogDAO;
import org.example.database.DBManager;
import org.example.models.Log;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SQLiteLogDAO implements LogDAO {

    @Override
    public void save(Log log) {
        // created_at fills automatically by DEFAULT CURRENT_TIMESTAMP
        String sql = "INSERT INTO logs (level, message) VALUES (?, ?)";
        try (Connection connection = DBManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, log.getLevel());
            statement.setString(2, log.getMessage());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Log> findAll() {
        List<Log> logs = new ArrayList<>();
        String sql = "SELECT * FROM logs ORDER BY created_at DESC";
        try (Connection connection = DBManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                logs.add(mapResultSetToLog(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }

    private Log mapResultSetToLog(ResultSet rs) throws SQLException {
        Log log = new Log();
        log.setId(rs.getLong("log_id"));
        log.setLevel(rs.getString("level"));
        log.setMessage(rs.getString("message"));
        Timestamp timestamp = rs.getTimestamp("created_at");
        if (timestamp != null) {
            log.setCreatedAt(timestamp.toLocalDateTime());
        }
        return log;
    }
}