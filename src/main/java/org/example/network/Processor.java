package org.example.network;

import org.example.models.User;
import org.example.models.Chat;
import org.example.models.ChatMember;
import org.example.models.ChatType;
import org.example.models.ChatRole;
import org.example.DAO.impl.*;

import org.example.protocol.CommandType;
import org.example.protocol.Message;
import org.example.protocol.MessagePacket;
import org.example.service.AuthService;
import org.example.service.MessageService;
import org.example.service.ChatService;
import org.example.service.FileService;

import java.util.List;
import java.util.Optional;

public class Processor {
    private final AuthService authService;
    private final MessageService messageService;
    private final ChatService chatService;
    private final FileService fileService;

    public Processor() {
        this.authService = new AuthService(new SQLiteUserDAO());
        this.messageService = new MessageService(new SQLiteMessageDAO());
        this.chatService = new ChatService(new SQLiteChatDAO(), new SQLiteChatMemberDAO());
        this.fileService = new FileService(new SQLiteAttachmentDAO());
    }

    public synchronized Message process(Message message, int currentUserId) {
        CommandType command = message.getCommandType();
        if (command == null) {
            return new Message(CommandType.STATUS_ERROR, 0, "ERROR:UNKNOWN_COMMAND");
        }

        try {
            switch (command) {
                case LOGIN -> {
                    Optional<User> userOpt = authService.loginFromRaw(message.getText());
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        System.out.println("[PROCESSOR] Authorization successful for: " + user.getUsername());
                        return new Message(CommandType.STATUS_OK, (int) user.getUser_id(), "SUCCESS;" + user.getRole());
                    } else {
                        return new Message(CommandType.STATUS_ERROR, 0, "ERROR:INVALID_CREDENTIALS");
                    }
                }

                case REGISTER -> {
                    Optional<User> registeredUser = authService.registerFromRaw(message.getText());
                    if (registeredUser.isPresent()) {
                        System.out.println("[PROCESSOR]\n" + "The new user has been successfully registered.");
                        return new Message(CommandType.STATUS_OK, (int) registeredUser.get().getUser_id(), "SUCCESS:REGISTRATION_COMPLETED");
                    } else {
                        return new Message(CommandType.STATUS_ERROR, 0, "ERROR:USERNAME_OR_PHONE_ALREADY_EXISTS");
                    }
                }

                case SEARCH -> {
                    SQLiteUserDAO userDao = new SQLiteUserDAO();
                    String query = message.getText().trim();
                    Optional<User> foundUser;

                    if (query.matches("^\\+?\\d+$")) {
                        foundUser = userDao.findByPhone(query);
                    } else {
                        foundUser = userDao.findByUsername(query);
                    }

                    if (foundUser.isPresent()) {
                        User u = foundUser.get();

                        if (u.getUser_id() == currentUserId) {
                            return new Message(CommandType.STATUS_ERROR, 0, "ERROR:CANNOT_ADD_YOURSELF");
                        }

                        SQLiteChatDAO chatDao = new SQLiteChatDAO();
                        SQLiteChatMemberDAO memberDao = new SQLiteChatMemberDAO();

                        boolean chatExists = false;
                        List<ChatMember> myChats = memberDao.findByUserId(currentUserId);
                        for (ChatMember cm : myChats) {
                            Optional<ChatMember> contactInSameChat = memberDao.findMember(cm.getChatId(), u.getUser_id());
                            if (contactInSameChat.isPresent()) {
                                chatExists = true;
                                break;
                            }
                        }

                        if (!chatExists) {
                            Chat newChat = new Chat();
                            newChat.setName(u.getUsername());
                            newChat.setType(ChatType.PRIVATE);
                            newChat = chatDao.save(newChat);

                            ChatMember me = new ChatMember();
                            me.setChatId(newChat.getId());
                            me.setUserId(currentUserId);
                            me.setRole(ChatRole.USER);
                            memberDao.addMember(me);

                            ChatMember contact = new ChatMember();
                            contact.setChatId(newChat.getId());
                            contact.setUserId(u.getUser_id());
                            contact.setRole(ChatRole.USER);
                            memberDao.addMember(contact);

                            System.out.println("[PROCESSOR] Created new private chat with: " + u.getUsername());
                        }

                        return new Message(CommandType.STATUS_OK, (int) u.getUser_id(), u.getUsername());
                    } else {
                        return new Message(CommandType.STATUS_ERROR, 0, "ERROR:USER_NOT_FOUND");
                    }
                }

                case GET_CHATS -> {
                    if (currentUserId == -1) return new Message(CommandType.STATUS_ERROR, 0, "ERROR:NOT_AUTHORIZED");

                    SQLiteChatMemberDAO memberDao = new SQLiteChatMemberDAO();
                    SQLiteChatDAO chatDao = new SQLiteChatDAO();

                    List<ChatMember> myMemberships = memberDao.findByUserId(currentUserId);
                    StringBuilder sb = new StringBuilder();

                    for (ChatMember cm : myMemberships) {
                        Optional<Chat> c = chatDao.findById(cm.getChatId());
                        if (c.isPresent()) {
                            sb.append(c.get().getName()).append(";");
                        }
                    }

                    String result = sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";
                    return new Message(CommandType.STATUS_OK, 0, result);
                }

                case SEND_MESSAGE -> {
                    try {
                        String[] parts = message.getText().split(";", 3);
                        if (parts.length >= 3) {
                            long chatId = Long.parseLong(parts[0]);
                            String textContent = parts[2];
                            org.example.models.Message msg = new org.example.models.Message();
                            msg.setChatId(chatId);
                            msg.setSenderId(currentUserId);
                            msg.setContent(textContent);
                            msg.setStatus("SENT");
                            msg.setType(org.example.models.MessageType.TEXT);
                            msg.setDeleted(false);

                            new org.example.DAO.impl.SQLiteMessageDAO().save(msg);
                            return new Message(CommandType.STATUS_OK, (int) chatId, "SUCCESS:DELIVERED");
                        }
                        return new Message(CommandType.STATUS_ERROR, 0, "ERROR:BAD_FORMAT");
                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, 0, "ERROR:FAILED_TO_SEND");
                    }
                }
                case GET_CHAT_HISTORY -> {
                    if (currentUserId == -1) return new Message(CommandType.STATUS_ERROR, 0, "ERROR:NOT_AUTHORIZED");

                    String chatName = message.getText().trim();
                    org.example.DAO.impl.SQLiteChatDAO chatDao = new org.example.DAO.impl.SQLiteChatDAO();
                    org.example.DAO.impl.SQLiteChatMemberDAO memberDao = new org.example.DAO.impl.SQLiteChatMemberDAO();
                    org.example.DAO.impl.SQLiteMessageDAO msgDao = new org.example.DAO.impl.SQLiteMessageDAO();
                    org.example.DAO.impl.SQLiteUserDAO userDao = new org.example.DAO.impl.SQLiteUserDAO();

                    long targetChatId = -1;
                    for (org.example.models.ChatMember cm : memberDao.findByUserId(currentUserId)) {
                        java.util.Optional<org.example.models.Chat> c = chatDao.findById(cm.getChatId());
                        if (c.isPresent() && c.get().getName().equals(chatName)) {
                            targetChatId = c.get().getId();
                            break;
                        }
                    }

                    if (targetChatId == -1) return new Message(CommandType.STATUS_ERROR, 0, "ERROR:CHAT_NOT_FOUND");

                    java.util.List<org.example.models.Message> msgs = msgDao.findByChatId(targetChatId);
                    StringBuilder sb = new StringBuilder();

                    for (org.example.models.Message m : msgs) {
                        String typeStr = m.getType() != null ? m.getType().name() : "TEXT";
                        long msgId = m.getId();
                        String safeContent = m.getContent() != null ? m.getContent() : "";

                        if (m.getSenderId() == currentUserId) {
                            sb.append("MINE:::").append(typeStr).append(":::").append(msgId).append(":::").append(safeContent).append("|||");
                        } else {
                            String senderName = userDao.findById(m.getSenderId()).map(org.example.models.User::getUsername).orElse("User");
                            sb.append(senderName).append(":::").append(typeStr).append(":::").append(msgId).append(":::").append(safeContent).append("|||");
                        }
                    }
                    return new Message(CommandType.STATUS_OK, (int) targetChatId, sb.toString());
                }

                case DOWNLOAD_FILE -> {
                    try {
                        long msgId = Long.parseLong(message.getText());
                        org.example.DAO.impl.SQLiteAttachmentDAO attDao = new org.example.DAO.impl.SQLiteAttachmentDAO();
                        java.util.List<org.example.models.Attachment> attachments = attDao.findByMessageId(msgId);

                        if (!attachments.isEmpty()) {
                            org.example.models.Attachment att = attachments.get(0);
                            java.io.File file = new java.io.File(att.getFilePath());

                            if (file.exists()) {
                                byte[] fileBytes = java.nio.file.Files.readAllBytes(file.toPath());
                                String base64Data = java.util.Base64.getEncoder().encodeToString(fileBytes);
                                return new Message(CommandType.STATUS_OK, 0, att.getFileName() + ";" + base64Data);
                            }
                        }
                        return new Message(CommandType.STATUS_ERROR, 0, "ERROR:FILE_NOT_FOUND");
                    } catch (Exception e) {
                        e.printStackTrace();
                        return new Message(CommandType.STATUS_ERROR, 0, "ERROR:DOWNLOAD_FAILED");
                    }
                }
                case SEND_FILE -> {
                    try {
                        String[] parts = message.getText().split(";", 3);
                        if (parts.length >= 3) {
                            long chatId = Long.parseLong(parts[0]);
                            String fileName = parts[1];
                            String base64Data = parts[2];

                            byte[] fileBytes = java.util.Base64.getDecoder().decode(base64Data);

                            java.io.File uploadDir = new java.io.File("server_uploads");
                            if (!uploadDir.exists()) uploadDir.mkdir();

                            java.io.File savedFile = new java.io.File(uploadDir, System.currentTimeMillis() + "_" + fileName);
                            java.nio.file.Files.write(savedFile.toPath(), fileBytes);

                            org.example.models.Message msg = new org.example.models.Message();
                            msg.setChatId(chatId);
                            msg.setSenderId(currentUserId);
                            msg.setContent("📎 " + fileName);
                            msg.setStatus("SENT");
                            msg.setType(org.example.models.MessageType.FILE);
                            msg.setDeleted(false);

                            msg = new org.example.DAO.impl.SQLiteMessageDAO().save(msg);

                            org.example.models.Attachment attachment = new org.example.models.Attachment();
                            attachment.setMessageId(msg.getId());
                            attachment.setFileName(fileName);
                            attachment.setFilePath(savedFile.getAbsolutePath());
                            attachment.setFileSize(fileBytes.length);

                            new org.example.DAO.impl.SQLiteAttachmentDAO().save(attachment);

                            return new Message(CommandType.STATUS_OK, (int) chatId, "SUCCESS:FILE_DELIVERED");
                        }
                        return new Message(CommandType.STATUS_ERROR, 0, "ERROR:BAD_FORMAT");
                    } catch (Exception e) {
                        e.printStackTrace();
                        return new Message(CommandType.STATUS_ERROR, 0, "ERROR:FAILED_TO_SAVE_FILE");
                    }
                }
                case GET_CONTACTS -> {
                    if (currentUserId == -1) return new Message(CommandType.STATUS_ERROR, 0, "ERROR:NOT_AUTHORIZED");
                    StringBuilder sb = new StringBuilder();

                    try (java.sql.Connection conn = org.example.database.DBManager.getConnection();
                         java.sql.PreparedStatement pstmt = conn.prepareStatement("SELECT user_id, username, status FROM users WHERE user_id != ?")) {

                        pstmt.setInt(1, currentUserId);
                        java.sql.ResultSet rs = pstmt.executeQuery();

                        while (rs.next()) {
                            int id = rs.getInt("user_id");
                            String name = rs.getString("username");
                            String status = rs.getString("status");
                            sb.append(id).append(":::").append(name).append(":::").append(status).append("|||");
                        }
                        return new Message(CommandType.STATUS_OK, 0, sb.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                        return new Message(CommandType.STATUS_ERROR, 0, "ERROR:DB_FAIL");
                    }
                }

                case CREATE_GROUP -> {
                    if (currentUserId == -1) return new Message(CommandType.STATUS_ERROR, 0, "ERROR:NOT_AUTHORIZED");
                    String[] parts = message.getText().split(";", 2);
                    if (parts.length < 2) return new Message(CommandType.STATUS_ERROR, 0, "ERROR:BAD_FORMAT");

                    String groupName = parts[0];
                    String[] userIds = parts[1].split(",");

                    org.example.DAO.impl.SQLiteChatDAO chatDao = new org.example.DAO.impl.SQLiteChatDAO();
                    org.example.DAO.impl.SQLiteChatMemberDAO memberDao = new org.example.DAO.impl.SQLiteChatMemberDAO();

                    org.example.models.Chat newGroup = new org.example.models.Chat();
                    newGroup.setName(groupName);
                    newGroup.setType(org.example.models.ChatType.GROUP);
                    newGroup = chatDao.save(newGroup);

                    org.example.models.ChatMember creator = new org.example.models.ChatMember();
                    creator.setChatId(newGroup.getId());
                    creator.setUserId(currentUserId);
                    creator.setRole(org.example.models.ChatRole.ADMIN);
                    memberDao.addMember(creator);

                    for (String uidStr : userIds) {
                        try {
                            int uid = Integer.parseInt(uidStr.trim());
                            org.example.models.ChatMember member = new org.example.models.ChatMember();
                            member.setChatId(newGroup.getId());
                            member.setUserId(uid);
                            member.setRole(org.example.models.ChatRole.USER);
                            memberDao.addMember(member);
                        } catch (Exception ignored) {}
                    }
                    return new Message(CommandType.STATUS_OK, 0, "SUCCESS:GROUP_CREATED");
                }
                default -> {
                    return new Message(CommandType.STATUS_ERROR, 0, "ERROR:NOT_IMPLEMENTED");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(CommandType.STATUS_ERROR, 0, "ERROR:" + e.getMessage().toUpperCase());
        }
    }
}