package org.example.network;

import org.example.DAO.impl.*;
import org.example.models.User;
import org.example.models.Chat;
import org.example.models.ChatMember;
import org.example.protocol.CommandType;
import org.example.protocol.Message;
import org.example.protocol.MessagePacket;
import org.example.service.*;

import java.io.File;
import java.nio.file.Files;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

public class Processor {
    private final AuthService authService;
    private final MessageService messageService;
    private final ChatService chatService;
    private final FileService fileService;
    private final SQLiteUserDAO userDAO;
    private final LogAndStatsService logAndStatsService;

    public Processor() {
        this.userDAO = new SQLiteUserDAO();
        this.authService = new AuthService(this.userDAO);
        this.messageService = new MessageService(new SQLiteMessageDAO());
        this.chatService = new ChatService(new SQLiteChatDAO(), new SQLiteChatMemberDAO());
        this.fileService = new FileService(new SQLiteAttachmentDAO());
        this.logAndStatsService = new LogAndStatsService(new SQLiteLogDAO(), userDAO, new SQLiteChatDAO(), new SQLiteMessageDAO());
    }
    public void broadcastLog(String logMessage) {
        Message msg = new Message(CommandType.NEW_LOG, 0, logMessage);
        MessagePacket packet = new MessagePacket((byte) 0, System.currentTimeMillis(), msg);

        for (Integer uid : ClientRegistry.activeClients.keySet()) {
            ClientHandler handler = ClientRegistry.getHandler(uid);
            String role = authService.getUserRole(uid);

            if (handler != null && "ADMIN".equalsIgnoreCase(role)) {
                handler.sendPacket(packet);
            }
        }
    }

    public synchronized Message process(Message message) {
        try {
            switch (message.getCommandType()) {
                case LOGIN -> {
                    Optional<User> userOpt = authService.loginFromRaw(message.getText());
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        logAndStatsService.logSystemEvent("INFO", "User ID " + user.getUser_id() + " logged into the system.");
                        broadcastLog("User ID " + user.getUser_id() + " logged into the system.");
                        return new Message(CommandType.STATUS_OK, (int) user.getUser_id(), "SUCCESS;" + user.getRole());
                    } else {
                        return new Message(CommandType.STATUS_ERROR, 0, "ERROR:INVALID_USERNAME_OR_PASSWORD_OR_BLOCKED");
                    }
                }

                case REGISTER -> {
                    String[] parts = message.getText().split(";");
                    String username = parts[0];
                    Optional<User> registeredUserOpt = authService.registerFromRaw(message.getText());
                    if (registeredUserOpt.isPresent()) {
                        String log = "User " + username + " registered a new account.";
                        logAndStatsService.logSystemEvent("INFO", log);
                        broadcastLog(log);
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
                        String response = chatService.getUserChats(message.getUserId());
                        return new Message(CommandType.STATUS_OK, message.getUserId(), response);
                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:FAILED_TO_LOAD_CHATS");
                    }
                }

                case MARK_AS_READ -> {
                    try {
                        int chatId = Integer.parseInt(message.getText().trim());
                        messageService.markChatAsRead(chatId, message.getUserId());
                        return new Message(CommandType.STATUS_OK, message.getUserId(), "SILENT_OK");
                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:MARK_READ_FAILED");
                    }
                }

                case SEARCH -> {
                    try {
                        String identifier = message.getText().trim();
                        Optional<User> targetUserOpt = userDAO.findByUsername(identifier);
                        if (targetUserOpt.isEmpty()) targetUserOpt = userDAO.findByPhone(identifier);

                        if (targetUserOpt.isEmpty()) {
                            return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:USER_NOT_FOUND");
                        }

                        User targetUser = targetUserOpt.get();
                        long myId = message.getUserId();
                        long peerId = targetUser.getUser_id();

                        if (peerId == myId) {
                            return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:CANNOT_ADD_YOURSELF");
                        }
                        if (chatService.privateChatExists(myId, peerId)) {
                            return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:PRIVATE_CHAT_ALREADY_EXISTS");
                        }
                        Chat privateChat = chatService.createChat("PrivateChat", org.example.models.ChatType.PRIVATE, myId);
                        chatService.addUserToChat(privateChat.getId(), peerId);

                        ClientHandler peerHandler = ClientRegistry.getHandler((int) peerId);
                        if (peerHandler != null) {
                            peerHandler.sendPacket(new MessagePacket((byte) 0, System.currentTimeMillis(),
                                    new Message(CommandType.STATUS_OK, (int) peerId, "SUCCESS:REFRESH_CHATS")));
                        }

                        return new Message(CommandType.STATUS_OK, message.getUserId(), "SUCCESS:REFRESH_CHATS");
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
                        String log = "User ID " + message.getUserId() + " created a new group: " + groupName + " (ID: " + createdChat.getId() + ").";
                        logAndStatsService.logSystemEvent("INFO", log);
                        broadcastLog(log);
                        return new Message(CommandType.STATUS_OK, message.getUserId(), "SUCCESS:GROUP_CREATED");
                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:FAILED_TO_CREATE_CHAT");
                    }
                }

                case GET_CHAT_HISTORY -> {
                    try {
                        int chatId = Integer.parseInt(message.getText());
                        String history = chatService.getChatHistory(chatId);
                        return new Message(CommandType.GET_CHAT_HISTORY, chatId, history);
                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:FAILED_TO_LOAD_HISTORY");
                    }
                }
                case GET_PROFILE -> {
                    String profileData = authService.getUserProfileRaw(message.getUserId());
                    if (profileData.startsWith("ERROR")) {
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), profileData);
                    }
                    return new Message(CommandType.STATUS_OK, message.getUserId(), profileData);
                }

                case UPDATE_PROFILE -> {
                    try {
                        String[] tokens = message.getText().split(";");
                        String newUsername = tokens[0].trim();
                        String newPhone = tokens[1].trim();

                        String result = authService.updateProfile(message.getUserId(), newUsername, newPhone);
                        if (result.startsWith("ERROR")) {
                            return new Message(CommandType.STATUS_ERROR, message.getUserId(), result);
                        }
                        return new Message(CommandType.STATUS_OK, message.getUserId(), result);
                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:BAD_REQUEST");
                    }
                }

                case DELETE_ACCOUNT -> {
                    String deleteResult = userDAO.deleteUserAccountFully(message.getUserId());
                    if (!deleteResult.startsWith("ERROR")) {
                        String log = "User ID " + message.getUserId() + " deleted their account permanently.";
                        logAndStatsService.logSystemEvent("WARN", log);
                        broadcastLog(log);
                    }
                    return new Message(CommandType.STATUS_OK, message.getUserId(), deleteResult);
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
                        long lastMsgId = messageService.getLastMessageId();
                        fileService.registerAttachment(lastMsgId, fileName, serverFile.getAbsolutePath(), fileBytes.length);
                        String log = "User " + message.getUserId() + " sent file: " + fileName + " in chat " + chatId;
                        logAndStatsService.logSystemEvent("INFO", log);
                        broadcastLog(log);
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
                        for (ChatMember member : members) {
                            if (member.getUserId() == message.getUserId()) continue;
                            ClientHandler peerHandler = ClientRegistry.getHandler((int) member.getUserId());
                            if (peerHandler != null) {
                                peerHandler.sendPacket(new MessagePacket((byte) 0, System.currentTimeMillis(),
                                        new Message(CommandType.STATUS_OK, (int) member.getUserId(), "SUCCESS:REFRESH_CHATS")));
                            }
                        }
                        String log = "Admin " + message.getUserId() + " deleted chat ID " + chatId + ".";
                        logAndStatsService.logSystemEvent("WARN", log);
                        broadcastLog(log);
                        return new Message(CommandType.STATUS_OK, message.getUserId(), "SUCCESS:REFRESH_CHATS");
                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:FAILED_TO_DELETE_CHAT");
                    }
                }

                case GET_GROUP_MEMBERS -> {
                    try {
                        int chatId = Integer.parseInt(message.getText().trim());
                        String responseText = chatService.getGroupMembersData(chatId);
                        return new Message(CommandType.STATUS_OK, chatId, responseText);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:FAILED_TO_LOAD_MEMBERS");
                    }
                }
                case ADD_GROUP_MEMBER -> {
                    try {
                        String[] parts = message.getText().split(";", 2);
                        int chatId = Integer.parseInt(parts[0]);
                        String targetIdentifier = parts[1].trim();
                        Optional<User> targetUserOpt = userDAO.findByUsername(targetIdentifier);
                        if (targetUserOpt.isEmpty()) targetUserOpt = userDAO.findByPhone(targetIdentifier);

                        if (targetUserOpt.isEmpty()) {
                            return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:USER_NOT_FOUND");
                        }
                        User targetUser = targetUserOpt.get();
                        if (chatService.isMember(chatId, targetUser.getUser_id())) {
                            return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:ALREADY_IN_CHAT");
                        }
                        chatService.addUserToChat(chatId, targetUser.getUser_id());
                        String log = "User " + message.getUserId() + " added " + targetUser.getUsername() + " to chat " + chatId;
                        logAndStatsService.logSystemEvent("INFO", log);
                        broadcastLog(log);
                        ClientHandler peerHandler = ClientRegistry.getHandler((int) targetUser.getUser_id());
                            if (peerHandler != null) {
                                peerHandler.sendPacket(new MessagePacket((byte) 0, System.currentTimeMillis(),
                                        new Message(CommandType.STATUS_OK, (int) targetUser.getUser_id(), "SUCCESS:REFRESH_CHATS")));
                            }
                            return new Message(CommandType.STATUS_OK, chatId, "SUCCESS:USER_ADDED_TO_GROUP");

                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:ADD_MEMBER_FAILED");
                    }
                }
                case PROMOTE_TO_ADMIN -> {
                    try {
                        String[] tokens = message.getText().split(";");
                        int chatId = Integer.parseInt(tokens[0]);
                        String targetUsername = tokens[1].trim();

                        if (!chatService.isAdminOfChat(message.getUserId(), chatId)) {
                            return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:NOT_AN_ADMIN");
                        }
                        Optional<User> targetUser = userDAO.findByUsername(targetUsername);
                        if (targetUser.isEmpty()) {
                            return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:USER_NOT_FOUND");
                        }
                        int targetUserId = (int) targetUser.get().getUser_id();
                        chatService.promoteUserToAdmin(chatId, targetUserId);

                        String log = "User " + message.getUserId() + " promoted " + targetUsername + " to admin in chat " + chatId;
                        logAndStatsService.logSystemEvent("INFO", log);
                        broadcastLog(log);
                        return new Message(CommandType.STATUS_OK, chatId, "SUCCESS:USER_PROMOTED");
                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:PROMOTION_FAILED");
                    }
                }
                case LEAVE_CHAT -> {
                    try {
                        int chatId = Integer.parseInt(message.getText().trim());
                        if (chatService.isSoleAdmin(chatId, message.getUserId())) {
                            return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:SOLE_ADMIN");
                        }
                        chatService.leaveChat(chatId, message.getUserId());
                        String log = "User " + message.getUserId() + " left chat " + chatId;
                        logAndStatsService.logSystemEvent("INFO", log);
                        broadcastLog(log);
                        return new Message(CommandType.STATUS_OK, message.getUserId(), "SUCCESS:REFRESH_CHATS");
                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:FAILED_TO_LEAVE_CHAT");
                    }
                }
                case RENAME_CHAT -> {
                    try {
                        String[] tokens = message.getText().split(";");
                        int chatId = Integer.parseInt(tokens[0]);
                        String newName = tokens[1].trim();

                        if (!chatService.isAdminOfChat(message.getUserId(), chatId)) {
                            return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:NOT_AN_ADMIN");
                        }
                        chatService.renameChat(chatId, newName);
                        String log = "User " + message.getUserId() + " renamed chat " + chatId + " to '" + newName + "'";
                        logAndStatsService.logSystemEvent("INFO", log);
                        broadcastLog(log);
                        return new Message(CommandType.STATUS_OK, chatId, "SUCCESS:RENAME;" + newName);
                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:RENAME_FAILED");
                    }
                }
                case REMOVE_GROUP_MEMBER -> {
                    try {
                        String[] tokens = message.getText().split(";");
                        int chatId = Integer.parseInt(tokens[0]);
                        String targetUsername = tokens[1].trim();

                        if (!chatService.isAdminOfChat(message.getUserId(), chatId)) {
                            return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:NOT_AN_ADMIN");
                        }

                        Optional<User> targetUser = userDAO.findByUsername(targetUsername);
                        if (targetUser.isEmpty()) {
                            return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:USER_NOT_FOUND");
                        }

                        int targetUserId = (int) targetUser.get().getUser_id();

                        if (targetUserId == message.getUserId()) {
                            return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:CANNOT_REMOVE_SELF");
                        }
                        chatService.removeUserFromGroup(chatId, targetUserId);

                        ClientHandler peerHandler = ClientRegistry.getHandler(targetUserId);
                        if (peerHandler != null) {
                            peerHandler.sendPacket(new MessagePacket((byte) 0, System.currentTimeMillis(),
                                    new Message(CommandType.STATUS_OK, targetUserId, "SUCCESS:REFRESH_CHATS")));
                        }
                        broadcastLog("Admin " + message.getUserId() + " removed " + targetUsername + " from chat " + chatId);
                        return new Message(CommandType.STATUS_OK, chatId, "SUCCESS:USER_REMOVED");
                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:REMOVE_FAILED");
                    }
                }

                case GET_ADMIN_STATS -> {
                    try {
                        long onlineCount = logAndStatsService.getOnlineUsersCount();
                        int totalUsers = logAndStatsService.getTotalUsersCount();
                        long totalMessages = messageService.getTotalMessagesCount();
                        String stats = onlineCount + ";" + totalUsers + ";" + totalMessages;
                        return new Message(CommandType.GET_ADMIN_STATS, message.getUserId(), stats);
                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:STATS_FAILED");
                    }
                }

                case GET_ALL_USERS -> {
                    try {
                        List<User> users = userDAO.findAll();
                        StringBuilder sb = new StringBuilder();

                        for (User u : users) {
                            sb.append(u.getUser_id()).append(",")
                                    .append(u.getStatus()).append(",")
                                    .append(u.getRole()).append(",")
                                    .append(u.isBlocked()).append(";");
                        }
                        return new Message(CommandType.GET_ALL_USERS, message.getUserId(), sb.toString());
                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:USERS_FAILED");
                    }
                }

                case ADMIN_ACTION_ROLE -> {
                    try {
                        String[] tokens = message.getText().split(";");
                        int targetId = Integer.parseInt(tokens[0]);
                        String newRole = tokens[1];
                        userDAO.updateUserRole(targetId, newRole);

                        String log = "Admin " + message.getUserId() + " changed role of user " + targetId + " to " + newRole;
                        logAndStatsService.logSystemEvent("INFO", log);
                        broadcastLog(log);
                        return new Message(CommandType.STATUS_OK, message.getUserId(), "SUCCESS:ROLE_CHANGED");
                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:ROLE_CHANGE_FAILED");
                    }
                }

                case ADMIN_ACTION_BLOCK -> {
                    try {
                        String[] tokens = message.getText().split(";");
                        int targetId = Integer.parseInt(tokens[0]);
                        boolean isBlocked = Boolean.parseBoolean(tokens[1]);
                        userDAO.updateBlockStatus(targetId, isBlocked);

                        String log = "Admin " + message.getUserId() + (isBlocked ? " blocked " : " unblocked ") + "user " + targetId;
                        logAndStatsService.logSystemEvent("INFO", log);
                        broadcastLog(log);

                        return new Message(CommandType.STATUS_OK, message.getUserId(), "SUCCESS:USER_BLOCKED");
                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:BLOCK_FAILED");
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