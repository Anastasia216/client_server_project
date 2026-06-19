package org.example.network;

import org.example.DAO.impl.SQLiteMessageDAO;
import org.example.DAO.impl.SQLiteUserDAO;
import org.example.models.User;
import org.example.protocol.CommandType;
import org.example.protocol.Message;
import org.example.protocol.MessagePacket;
import org.example.service.AuthService;
import org.example.service.MessageService;

import java.util.List;
import java.util.Optional;

public class Processor {
    private final AuthService authService;
    private final MessageService messageService;

    public Processor() {
        this.authService = new AuthService(new SQLiteUserDAO());
        this.messageService = new MessageService(new SQLiteMessageDAO());
    }

    public synchronized Message process(Message message) {
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
                        System.out.println("[PROCESSOR]\n" + "Authorization successful for: " + user.getUsername());
                        return new Message(CommandType.STATUS_OK, (int) user.getUser_id(), "SUCCESS;" + user.getRole());
                    } else {
                        return new Message(CommandType.STATUS_ERROR, 0, "ERROR:INVALID_USERNAME_OR_PASSWORD_OR_BLOCKED");
                    }
                }

                case REGISTER -> {
                    boolean success = authService.registerFromRaw(message.getText());
                    if (success) {
                        System.out.println("[PROCESSOR]\n" + "The new user has been successfully registered.");
                        return new Message(CommandType.STATUS_OK, 0, "SUCCESS:REGISTRATION_COMPLETED");
                    } else {
                        return new Message(CommandType.STATUS_ERROR, 0, "ERROR:USERNAME_OR_EMAIL_ALREADY_EXISTS_OR_INVALID");
                    }
                }

                case SEND_MESSAGE -> {
                    try {
                        String[] tokens = message.getText().split(";", 3);
                        if (tokens.length < 3) {
                            return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:INVALID_FORMAT");
                        }

                        int chatId = Integer.parseInt(tokens[0]);
                        int receiverId = Integer.parseInt(tokens[1]);
                        String textContent = tokens[2];

                        messageService.sendMessageFromRaw(message.getUserId(), message.getText());

                        if (receiverId != 0) {
                            //приватне
                            ClientHandler receiverHandler = ClientRegistry.getHandler(receiverId);
                            if (receiverHandler != null) {
                                String forwardText = chatId + ";" + message.getUserId() + ";" + textContent;
                                receiverHandler.sendPacket(new MessagePacket((byte) 0, System.currentTimeMillis(),
                                        new Message(CommandType.SEND_MESSAGE, message.getUserId(), forwardText)));
                            }
                        } else {
                            //групове
                            List<Integer> groupMembers = List.of(1, 2, 3);

                            String forwardText = chatId + ";" + message.getUserId() + ";" + textContent;
                            Message groupMessage = new Message(CommandType.SEND_MESSAGE, message.getUserId(), forwardText);
                            MessagePacket groupPacket = new MessagePacket((byte) 0, System.currentTimeMillis(), groupMessage);

                            // цикл по всіх учасниках гурпи
                            for (int memberId : groupMembers) {
                                // виключаємо автора
                                if (memberId == message.getUserId()) continue;

                                ClientHandler memberHandler = ClientRegistry.getHandler(memberId);
                                if (memberHandler != null) {
                                    memberHandler.sendPacket(groupPacket);
                                }
                            }
                            System.out.println("[PROCESSOR] Group message from User " + message.getUserId() + " broadcasted to Chat " + chatId);
                        }

                        return new Message(CommandType.STATUS_OK, message.getUserId(), "SUCCESS:MESSAGE_PROCESSED");

                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:FAILED_TO_SEND");
                    }
                }

                case CREATE_CHAT -> {
                    try {
                        String chatName = message.getText();

                        if (chatName == null || chatName.trim().isEmpty()) {
                            return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:CHAT_NAME_EMPTY");
                        }

                        int simulatedChatId = 999;

                        System.out.println("[PROCESSOR] Group chat '" + chatName + "' created by User ID " + message.getUserId());
                        return new Message(CommandType.STATUS_OK, message.getUserId(), "SUCCESS:CHAT_CREATED;ID=" + simulatedChatId);

                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:FAILED_TO_CREATE_CHAT");
                    }
                }

                case ADD_USER_TO_CHAT -> {
                    try {
                        String[] tokens = message.getText().split(";");
                        if (tokens.length < 2) {
                            return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:INVALID_FORMAT");
                        }

                        int chatId = Integer.parseInt(tokens[0]);
                        int userToAddId = Integer.parseInt(tokens[1]);

                        System.out.println("[PROCESSOR] User " + userToAddId + " added to Chat " + chatId + " by User " + message.getUserId());
                        return new Message(CommandType.STATUS_OK, message.getUserId(), "SUCCESS:USER_ADDED_TO_CHAT");

                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:FAILED_TO_ADD_USER");
                    }
                }

                case GET_CHAT_HISTORY -> {
                    try {
                        int chatId = Integer.parseInt(message.getText());

                        String simulatedHistory = "User1: Hello!;User2: Hi there!;User1: How is the Java project going?";

                        System.out.println("[PROCESSOR] History requested for Chat ID " + chatId + " by User " + message.getUserId());
                        return new Message(CommandType.STATUS_OK, message.getUserId(), simulatedHistory);

                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:FAILED_TO_LOAD_HISTORY");
                    }
                }

                case SEND_FILE -> {
                    try {
                        String[] tokens = message.getText().split(";", 4);
                        if (tokens.length < 4) {
                            return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:INVALID_FILE_FORMAT");
                        }

                        int chatId = Integer.parseInt(tokens[0]);
                        int receiverId = Integer.parseInt(tokens[1]);
                        String fileName = tokens[2];
                        String base64Data = tokens[3];

                        byte[] fileBytes = java.util.Base64.getDecoder().decode(base64Data);

                        java.io.File uploadDir = new java.io.File("uploads");
                        if (!uploadDir.exists()) {
                            uploadDir.mkdirs();
                        }

                        java.io.File serverFile = new java.io.File(uploadDir, System.currentTimeMillis() + "_" + fileName);
                        java.nio.file.Files.write(serverFile.toPath(), fileBytes);
                        System.out.println("[PROCESSOR] File saved on server storage: " + serverFile.getAbsolutePath());

                        ClientHandler receiverHandler = ClientRegistry.getHandler(receiverId);
                        if (receiverHandler != null) {
                            String forwardText = chatId + ";" + message.getUserId() + ";" + fileName + ";" + base64Data;
                            Message fileMessage = new Message(CommandType.SEND_FILE, message.getUserId(), forwardText);
                            MessagePacket filePacket = new MessagePacket((byte) 0, System.currentTimeMillis(), fileMessage);

                            receiverHandler.sendPacket(filePacket);
                            System.out.println("[PROCESSOR] File forwarded in real-time to User " + receiverId);
                        }

                        return new Message(CommandType.STATUS_OK, message.getUserId(), "SUCCESS:FILE_TRANSFER_COMPLETED");

                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:FILE_UPLOAD_FAILED");
                    }
                }

                case BLOCK_USER -> {
                    try {
                        int userIdToBlock = Integer.parseInt(message.getText());
                        System.out.println("[ADMIN ACTION] User ID " + userIdToBlock + " has been blocked in the database.");

                        ClientHandler victimHandler = ClientRegistry.getHandler(userIdToBlock);
                        if (victimHandler != null) {
                            victimHandler.forceDisconnect();
                            System.out.println("[ADMIN ACTION] Active session for User ID " + userIdToBlock + " was forcefully terminated.");
                        }

                        return new Message(CommandType.STATUS_OK, message.getUserId(), "SUCCESS:USER_BLOCKED");

                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, message.getUserId(), "ERROR:FAILED_TO_BLOCK_USER");
                    }
                }

                default -> {
                    return new Message(CommandType.STATUS_ERROR, 0, "ERROR:NOT_IMPLEMENTED");
                }
            }
        } catch (Exception e) {
            return new Message(CommandType.STATUS_ERROR, 0, "ERROR:" + e.getMessage().toUpperCase());
        }
    }
}