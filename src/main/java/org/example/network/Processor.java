package org.example.network;

import org.example.DAO.Impl.SQLiteMessageDAO;
import org.example.DAO.Impl.SQLiteUserDAO;
import org.example.models.User;
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
        CommandType command = CommandType.fromId(message.cType);
        if (command == null) {
            return new Message(100, 0, "ERROR:UNKNOWN_COMMAND");
        }

        try {
            switch (command) {
                case LOGIN -> {
                    Optional<User> userOpt = authService.loginFromRaw(message.message);
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        System.out.println("[PROCESSOR]\n" + "Authorization successful for: " + user.getUsername());
                        return new Message(100, user.getUser_id(), "SUCCESS;" + user.getRole());
                    } else {
                        return new Message(100, 0, "ERROR:INVALID_USERNAME_OR_PASSWORD_OR_BLOCKED");
                    }
                }

                case REGISTER -> {
                    boolean success = authService.registerFromRaw(message.message);
                    if (success) {
                        System.out.println("[PROCESSOR]\n" + "The new user has been successfully registered.");
                        return new Message(100, 0, "SUCCESS:REGISTRATION_COMPLETED");
                    } else {
                        return new Message(100, 0, "ERROR:USERNAME_OR_EMAIL_ALREADY_EXISTS_OR_INVALID");
                    }
                }

                case SEND_MESSAGE -> {
                    try {
                        messageService.sendMessageFromRaw(message.bUserId, message.message);
                        return new Message(100, message.bUserId, "SUCCESS:MESSAGE_DELIVERED");
                    } catch (Exception e) {
                        return new Message(100, 0, "ERROR:FAILED_TO_SEND_MESSAGE");
                    }
                }

                default -> {
                    return new Message(100, 0, "ERROR:NOT_IMPLEMENTED");
                }
            }
        } catch (Exception e) {
            return new Message(100, 0, "ERROR:" + e.getMessage().toUpperCase());
        }
    }
}