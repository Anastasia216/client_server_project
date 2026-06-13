package org.example.database;
import java.sql.Statement;
import java.sql.Connection;


public class DatabaseInitializer {
    public static void initialize(){
        try(Connection conn = DBManager.getConnection();
        Statement statement = conn.createStatement()){
          statement.execute("""
                  CREATE TABLE IF NOT EXISTS users (
                  user_id INTEGER PRIMARY KEY AUTOINCREMENT,
                  username TEXT UNIQUE NOT NULL,
                  email TEXT UNIQUE NOT NULL,
                  password_hash TEXT NOT NULL,
                  role TEXT NOT NULL DEFAULT 'USER',
                  status TEXT DEFAULT 'OFFLINE',
                  last_seen DATETIME,
                  is_blocked INTEGER DEFAULT 0
                  );
                  """);
          statement.execute("""
                    CREATE TABLE IF NOT EXISTS chats (
                    chat_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    chat_name TEXT,
                    type TEXT NOT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    created_by INTEGER,
                    FOREIGN KEY(created_by)
                        REFERENCES users(user_id)
                    );
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS chat_members (
                        chat_id INTEGER,
                        user_id INTEGER,
                        role TEXT DEFAULT 'USER',
                        
                        PRIMARY KEY(chat_id, user_id),
                        
                        FOREIGN KEY(chat_id)
                        REFERENCES chats(id)
                        ON DELETE CASCADE,
                        
                        FOREIGN KEY(user_id)
                        REFERENCES users(id)
                        ON DELETE CASCADE
                    );
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS messages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        chat_id INTEGER NOT NULL,
                        sender_id INTEGER NOT NULL,
                        content TEXT,
                        sent_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        status TEXT DEFAULT 'SENT',
                        is_deleted INTEGER DEFAULT 0,
                        
                        FOREIGN KEY(chat_id)
                        REFERENCES chats(id)
                        ON DELETE CASCADE,
                        
                        FOREIGN KEY(sender_id)
                        REFERENCES users(id)
                    );
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS attachments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        message_id INTEGER NOT NULL,
                        file_name TEXT NOT NULL,
                        file_path TEXT NOT NULL,
                        file_size INTEGER,
                        upload_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                        
                        FOREIGN KEY(message_id)
                        REFERENCES messages(id)
                        ON DELETE CASCADE
                    );
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        level TEXT NOT NULL,
                        message TEXT NOT NULL,
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                    );
                    """);

            System.out.println("Database initialized.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

