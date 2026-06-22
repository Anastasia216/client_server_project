package org.example.network;

import org.example.DAO.impl.*;
import org.example.database.DBManager;
import org.example.models.User;
import org.example.models.Chat;
import org.example.models.ChatMember;
import org.example.protocol.CommandType;
import org.example.protocol.Message;
import org.example.protocol.MessagePacket;
import org.example.service.AuthService;
import org.example.service.MessageService;
import org.example.service.ChatService;
import org.example.service.FileService;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

public class Processor {
    private final AuthService authService;
    private final MessageService messageService;
    private final ChatService chatService;
    private final FileService fileService;
    private final SQLiteUserDAO userDAO;

    public Processor() {
        this.userDAO = new SQLiteUserDAO();
        this.authService = new AuthService(this.userDAO);
        this.messageService = new MessageService(new SQLiteMessageDAO());
        this.chatService = new ChatService(new SQLiteChatDAO(), new SQLiteChatMemberDAO());
        this.fileService = new FileService(new SQLiteAttachmentDAO());
    }

    public synchronized Message process(Message message) {
        try {
            switch (message.getCommandType()) {
                case LOGIN -> {
                    Optional<User> userOpt = authService.loginFromRaw(message.getText());
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        System.out.println("[PROCESSOR] Authorization successful for: " + user.getUsername());
                        return new Message(CommandType.STATUS_OK, (int) user.getUser_id(), "SUCCESS;" + user.getRole());
                    } else {
                        return new Message(CommandType.STATUS_ERROR, 0, "ERROR:INVALID_USERNAME_OR_PASSWORD_OR_BLOCKED");
                    }
                }

                case REGISTER -> {
                    Optional<User> registeredUserOpt = authService.registerFromRaw(message.getText());
                    if (registeredUserOpt.isPresent()) {
                        System.out.println("[PROCESSOR] New user successfully registered via Phone.");
                        return new Message(CommandType.STATUS_OK, 0, "SUCCESS:REGISTRATION_COMPLETED");
                    } else {
                        return new Message(CommandType.STATUS_ERROR, 0, "ERROR:USERNAME_OR_PHONE_ALREADY_EXISTS");
                    }
                }

                case SEND_MESSAGE -> {
                    try {
                        String[] tokens = message.getText().split(";", 3);
                        int chatId = Integer.parseInt(tokens[0]);
                        String textContent = tokens[2];
                        messageService.saveMessage(chatId, message.getUserId(), textContent);
                        List<ChatMember> members = chatService.getChatMembers(chatId);
                        String senderName = "Unknown";
                        Optional<User> senderOpt = userDAO.findById(message.getUserId());
                        if (senderOpt.isPresent()) {
                            senderName = senderOpt.get().getUsername();
                        }
                        Message broadcastMsg = new Message(CommandType.SEND_MESSAGE, message.getUserId(), chatId + ";" + senderName + ";" + textContent);
                        MessagePacket groupPacket = new MessagePacket((byte) 0, System.currentTimeMillis(), broadcastMsg);

                        for (ChatMember member : members) {
                            ClientHandler memberHandler = ClientRegistry.getHandler((int) member.getUserId());
                            if (memberHandler != null) {
                                memberHandler.sendPacket(groupPacket);
                            }
                        }

                        return new Message(CommandType.STATUS_OK, message.getUserId(), "SILENT_OK");
                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:FAILED_TO_SEND");
                    }
                }

                case GET_CHATS -> {
                    try {
                        List<Chat> userChats = chatService.getChatsForUser(message.getUserId());
                        StringBuilder sb = new StringBuilder();

                        for (Chat chat : userChats) {
                            String chatName = chat.getName();
                            if (chat.getType() == org.example.models.ChatType.PRIVATE) {
                                String sql = """
                                    SELECT username FROM users 
                                    JOIN chat_members ON users.user_id = chat_members.user_id 
                                    WHERE chat_members.chat_id = ? AND users.user_id != ?
                                """;
                                try (Connection conn = DBManager.getConnection();
                                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                                    stmt.setLong(1, chat.getId());
                                    stmt.setLong(2, message.getUserId());
                                    try (ResultSet rs = stmt.executeQuery()) {
                                        if (rs.next()) {
                                            chatName = rs.getString("username");
                                        }
                                    }
                                }
                            }
                            sb.append(chatName).append(" (ID: ").append(chat.getId()).append(");");
                        }
                        return new Message(CommandType.STATUS_OK, message.getUserId(), sb.toString());
                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:FAILED_TO_LOAD_CHATS");
                    }
                }

                case SEARCH -> {
                    try {
                        String identifier = message.getText().trim();
                        Optional<User> targetUserOpt = userDAO.findByUsername(identifier);
                        if (targetUserOpt.isEmpty()) {
                            targetUserOpt = userDAO.findByPhone(identifier);
                        }

                        if (targetUserOpt.isPresent()) {
                            User targetUser = targetUserOpt.get();
                            if (targetUser.getUser_id() == message.getUserId()) {
                                return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:CANNOT_ADD_YOURSELF");
                            }
                            Chat privateChat = chatService.createChat("PrivateChat", org.example.models.ChatType.PRIVATE, message.getUserId());
                            chatService.addUserToChat(privateChat.getId(), targetUser.getUser_id());
                            ClientHandler peerHandler = ClientRegistry.getHandler((int) targetUser.getUser_id());
                            if (peerHandler != null) {
                                peerHandler.sendPacket(new MessagePacket((byte) 0, System.currentTimeMillis(),
                                        new Message(CommandType.STATUS_OK, (int) targetUser.getUser_id(), "SUCCESS:REFRESH_CHATS")));
                            }
                            return new Message(CommandType.STATUS_OK, message.getUserId(), "SUCCESS:REFRESH_CHATS");
                        } else {
                            return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:USER_NOT_FOUND");
                        }
                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:SEARCH_FAILED");
                    }
                }

                case GET_CONTACTS -> {
                    try {
                        List<User> allUsers = userDAO.findAll();
                        StringBuilder sb = new StringBuilder();
                        for (User u : allUsers) {
                            if (u.getUser_id() == message.getUserId()) continue;
                            sb.append(u.getUser_id()).append(":::")
                                    .append(u.getUsername()).append(":::")
                                    .append(u.getStatus() != null ? u.getStatus() : "OFFLINE")
                                    .append("|||");
                        }
                        String responseText = sb.length() > 0 ? sb.substring(0, sb.length() - 3) : "";
                        return new Message(CommandType.STATUS_OK, message.getUserId(), responseText);
                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:FAILED_TO_LOAD_CONTACTS");
                    }
                }

                case CREATE_CHAT -> {
                    try {
                        String rawData = message.getText();
                        if (rawData == null || rawData.trim().isEmpty()) {
                            return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:DATA_EMPTY");
                        }
                        String[] tokens = rawData.split(";");
                        String groupName = tokens[0];
                        Chat createdChat = chatService.createGroup(groupName, message.getUserId());
                        List<Integer> addedUsers = new ArrayList<>();
                        for (int i = 1; i < tokens.length; i++) {
                            int targetUserId = Integer.parseInt(tokens[i]);
                            chatService.addUserToChat(createdChat.getId(), targetUserId);
                            addedUsers.add(targetUserId);
                        }
                        for (int uid : addedUsers) {
                            ClientHandler peerHandler = ClientRegistry.getHandler(uid);
                            if (peerHandler != null) {
                                peerHandler.sendPacket(new MessagePacket((byte) 0, System.currentTimeMillis(),
                                        new Message(CommandType.STATUS_OK, uid, "SUCCESS:REFRESH_CHATS")));
                            }
                        }
                        return new Message(CommandType.STATUS_OK, message.getUserId(), "SUCCESS:GROUP_CREATED");
                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:FAILED_TO_CREATE_CHAT");
                    }
                }

                case GET_CHAT_HISTORY -> {
                    try {
                        int chatId = Integer.parseInt(message.getText());
                        String sql = """
                            SELECT users.username, messages.content FROM messages 
                            JOIN users ON messages.sender_id = users.user_id 
                            WHERE messages.chat_id = ? 
                            ORDER BY messages.message_id ASC
                        """;
                        StringBuilder sb = new StringBuilder();
                        try (java.sql.Connection conn = org.example.database.DBManager.getConnection();
                             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
                            stmt.setInt(1, chatId);
                            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                                while (rs.next()) {
                                    sb.append(rs.getString("username")).append(":::").append(rs.getString("content")).append("\n");
                                }
                            }
                        }
                        return new Message(CommandType.GET_CHAT_HISTORY, chatId, sb.toString());
                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:FAILED_TO_LOAD_HISTORY");
                    }
                }

                case SEND_FILE -> {
                    try {
                        String[] tokens = message.getText().split(";", 4);
                        int chatId = Integer.parseInt(tokens[0]);
                        String fileName = tokens[2];
                        byte[] fileBytes = java.util.Base64.getDecoder().decode(tokens[3]);

                        File uploadDir = new File("uploads");
                        if (!uploadDir.exists()) uploadDir.mkdirs();

                        String uniqueName = System.currentTimeMillis() + "_" + fileName;
                        File serverFile = new File(uploadDir, uniqueName);
                        Files.write(serverFile.toPath(), fileBytes);

                        messageService.saveMessage(chatId, message.getUserId(), "FILE_ATTACHMENT:" + fileName + "?" + uniqueName);
                        long lastMsgId = 0;
                        try (java.sql.Connection conn = org.example.database.DBManager.getConnection();
                             java.sql.PreparedStatement stmt = conn.prepareStatement("SELECT max(message_id) FROM messages")) {
                            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                                if (rs.next()) lastMsgId = rs.getLong(1);
                            }
                        }

                        fileService.registerAttachment(lastMsgId, fileName, serverFile.getAbsolutePath(), fileBytes.length);

                        List<ChatMember> members = chatService.getChatMembers(chatId);
                        String senderName = "Unknown";
                        Optional<User> senderOpt = userDAO.findById(message.getUserId());
                        if (senderOpt.isPresent()) senderName = senderOpt.get().getUsername();
                        Message fileBroadcast = new Message(CommandType.SEND_FILE, message.getUserId(),
                                chatId + ";" + senderName + ";" + fileName + ";" + uniqueName);
                        MessagePacket filePacket = new MessagePacket((byte) 0, System.currentTimeMillis(), fileBroadcast);

                        for (ChatMember member : members) {
                            ClientHandler memberHandler = ClientRegistry.getHandler((int) member.getUserId());
                            if (memberHandler != null) {
                                memberHandler.sendPacket(filePacket);
                            }
                        }

                        return new Message(CommandType.STATUS_OK, message.getUserId(), "SILENT_OK");
                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:FILE_UPLOAD_FAILED");
                    }
                }

                case DOWNLOAD_FILE -> {
                    try {
                        String uniqueName = message.getText().trim();
                        File fileToSend = new File("uploads", uniqueName);
                        if (fileToSend.exists()) {
                            byte[] fileBytes = Files.readAllBytes(fileToSend.toPath());
                            String base64Data = java.util.Base64.getEncoder().encodeToString(fileBytes);
                            String originalName = uniqueName.contains("_") ? uniqueName.substring(uniqueName.indexOf("_") + 1) : uniqueName;
                            return new Message(CommandType.DOWNLOAD_FILE, message.getUserId(), originalName + ";" + base64Data);
                        } else {
                            return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:FILE_NOT_FOUND");
                        }
                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:DOWNLOAD_FAILED");
                    }
                }

                case DELETE_CHAT -> {
                    try {
                        int chatId = Integer.parseInt(message.getText().trim());
                        List<ChatMember> members = chatService.getChatMembers(chatId);
                        chatService.deleteChat(chatId);
                        System.out.println("[PROCESSOR] Chat ID " + chatId + " was permanently deleted via clean SQL CASCADE.");
                        for (ChatMember member : members) {
                            if (member.getUserId() == message.getUserId()) continue;
                            ClientHandler peerHandler = ClientRegistry.getHandler((int) member.getUserId());
                            if (peerHandler != null) {
                                peerHandler.sendPacket(new MessagePacket((byte) 0, System.currentTimeMillis(),
                                        new Message(CommandType.STATUS_OK, (int) member.getUserId(), "SUCCESS:REFRESH_CHATS")));
                            }
                        }
                        return new Message(CommandType.STATUS_OK, message.getUserId(), "SUCCESS:REFRESH_CHATS");
                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:FAILED_TO_DELETE_CHAT");
                    }
                }

                case GET_GROUP_MEMBERS -> {
                    try {
                        int chatId = Integer.parseInt(message.getText().trim());
                        String sql = """
                            SELECT users.user_id, users.username, chat_members.role FROM chat_members
                            JOIN users ON chat_members.user_id = users.user_id
                            WHERE chat_members.chat_id = ?
                            ORDER BY chat_members.role ASC, users.username ASC
                        """;

                        StringBuilder sb = new StringBuilder();
                        try (java.sql.Connection conn = org.example.database.DBManager.getConnection();
                             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
                            stmt.setInt(1, chatId);
                            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                                while (rs.next()) {
                                    sb.append(rs.getInt("user_id")).append(":::")
                                            .append(rs.getString("username")).append(":::")
                                            .append(rs.getString("role")).append("|||");
                                }
                            }
                        }
                        String responseText = sb.length() > 0 ? sb.substring(0, sb.length() - 3) : "";
                        return new Message(CommandType.STATUS_OK, chatId, responseText);
                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:FAILED_TO_LOAD_MEMBERS");
                    }
                }
                case PROMOTE_TO_ADMIN -> {
                    try {
                        String[] tokens = message.getText().split(";");
                        int chatId = Integer.parseInt(tokens[0]);
                        int targetUserId = Integer.parseInt(tokens[1]);
                        String checkSql = "SELECT role FROM chat_members WHERE chat_id = ? AND user_id = ?";
                        try (java.sql.Connection conn = org.example.database.DBManager.getConnection();
                             java.sql.PreparedStatement stmt = conn.prepareStatement(checkSql)) {
                            stmt.setInt(1, chatId);
                            stmt.setInt(2, message.getUserId());
                            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                                if (!rs.next() || !"ADMIN".equalsIgnoreCase(rs.getString("role"))) {
                                    return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:NOT_AN_ADMIN");
                                }
                            }
                        }
                        String updateSql = "UPDATE chat_members SET role = 'ADMIN' WHERE chat_id = ? AND user_id = ?";
                        try (java.sql.Connection conn = org.example.database.DBManager.getConnection();
                             java.sql.PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                            stmt.setInt(1, chatId);
                            stmt.setInt(2, targetUserId);
                            stmt.executeUpdate();
                        }
                        return new Message(CommandType.STATUS_OK, chatId, "SUCCESS:USER_PROMOTED");
                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:PROMOTION_FAILED");
                    }
                }
                case RENAME_CHAT -> {
                    try {
                        String[] tokens = message.getText().split(";", 2);
                        int chatId = Integer.parseInt(tokens[0]);
                        String newName = tokens[1].trim();
                        String checkSql = "SELECT role FROM chat_members WHERE chat_id = ? AND user_id = ?";
                        try (java.sql.Connection conn = org.example.database.DBManager.getConnection();
                             java.sql.PreparedStatement stmt = conn.prepareStatement(checkSql)) {
                            stmt.setInt(1, chatId);
                            stmt.setInt(2, message.getUserId());
                            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                                if (!rs.next() || !"ADMIN".equalsIgnoreCase(rs.getString("role"))) {
                                    return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:NOT_AN_ADMIN");
                                }
                            }
                        }
                        String updateSql = "UPDATE chats SET chat_name = ? WHERE chat_id = ?";
                        try (java.sql.Connection conn = org.example.database.DBManager.getConnection();
                             java.sql.PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                            stmt.setString(1, newName);
                            stmt.setInt(2, chatId);
                            stmt.executeUpdate();
                        }
                        return new Message(CommandType.STATUS_OK, chatId, "SUCCESS:RENAME;" + newName);
                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:RENAME_FAILED");
                    }
                }

                default -> {
                    return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:UNKNOWN_COMMAND");
                }
            }
        } catch (Exception e) {
            return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:INTERNAL_SERVER_ERROR");
        }
    }
}