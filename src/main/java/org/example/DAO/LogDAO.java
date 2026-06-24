package org.example.DAO;
import org.example.models.Log;

import java.util.List;

public interface LogDAO {
    void save(Log log);
    List<Log> findAll();
    void saveLog(String level, String message);
}
