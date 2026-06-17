package org.example.network;

import org.example.DAO.impl.SQLiteMessageDAO;
import org.example.DAO.impl.SQLiteUserDAO;
import org.example.models.User;
import org.example.protocol.CommandType;
import org.example.protocol.Message;
import org.example.service.AuthService;
import org.example.service.MessageService;

import java.util.Optional;

public class Processor {
    private final AuthService authService;
    private final MessageService messageService;

    public Processor() {
        this.authService = new AuthService(new SQLiteUserDAO());
        this.messageService = new MessageService(new SQLiteMessageDAO());
    }

    public synchronized Message process(Message message) {
        // Отримуємо CommandType напряму, бо getCommandType() повертає саме Enum
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
                        messageService.sendMessageFromRaw(message.getUserId(), message.getText());
                        return new Message(CommandType.STATUS_OK, message.getUserId(), "SUCCESS:MESSAGE_DELIVERED");
                    } catch (Exception e) {
                        return new Message(CommandType.STATUS_ERROR, 0, "ERROR:FAILED_TO_SEND_MESSAGE");
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