package org.example.network;

import javafx.application.Platform;
import org.example.protocol.Message;
import org.example.protocol.MessagePacket;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

public class NetworkReceiverThread implements Runnable {

    @Override
    public void run() {
        System.out.println("[RECEIVER_THREAD] Background receiver thread started.");
        InputStream in = NetworkClient.getInstance().getInputStream();
        try {
            while (NetworkClient.getInstance().isConnected() && !NetworkClient.getInstance().getSocket().isClosed()) {
                byte[] headerBase = in.readNBytes(MessagePacket.HEADER_SIZE);
                if (headerBase.length < MessagePacket.HEADER_SIZE) {
                    System.out.println("[RECEIVER_THREAD] Stream closed. Stopping thread.");
                    break;
                }
                int wLen = ByteBuffer.wrap(headerBase, 10, 4).getInt();
                int restSize = wLen + 2;
                byte[] restBytes = in.readNBytes(restSize);
                if (restBytes.length < restSize) {
                    System.out.println("[RECEIVER_THREAD] Incomplete payload. Stopping.");
                    break;
                }

                MessagePacket incomingPacket = MessagePacket.fromBytes(headerBase, restBytes);
                Message responseMessage = incomingPacket.getMessage();
                System.out.println("[RECEIVER_THREAD] Received packet: " + responseMessage.getCommandType());

                Platform.runLater(() -> {
                    Object controller = NetworkClient.getInstance().getActiveController();
                    if (controller == null) return;
                    String controllerName = controller.getClass().getSimpleName();
                    try {
                        switch (responseMessage.getCommandType()) {
                            case STATUS_OK, STATUS_ERROR -> {
                                if (controllerName.equals("LoginController") || controllerName.equals("RegisterController")) {
                                    Method method = controller.getClass().getMethod("handleAuthResponse", Message.class);
                                    method.invoke(controller, responseMessage);
                                } else if (controllerName.equals("ChatPanels") || controllerName.equals("AddGroupController") || controllerName.equals("AddContactController") || controllerName.equals("UserInfoController")) {
                                    Method method = controller.getClass().getMethod("handleSystemStatus", Message.class);
                                    method.invoke(controller, responseMessage);
                                }
                            }

                            case SEND_MESSAGE -> {
                                if (controllerName.equals("ChatPanels")) {
                                    Method method = controller.getClass().getMethod("handleIncomingMessage", Message.class);
                                    method.invoke(controller, responseMessage);
                                }
                            }

                            case SEND_FILE -> {
                                if (controllerName.equals("ChatPanels")) {
                                    Method method = controller.getClass().getMethod("handleIncomingFile", Message.class);
                                    method.invoke(controller, responseMessage);
                                }
                            }

                            case GET_CHAT_HISTORY -> {
                                if (controllerName.equals("ChatPanels")) {
                                    Method method = controller.getClass().getMethod("handleHistoryResponse", Message.class);
                                    method.invoke(controller, responseMessage);
                                }
                            }

                            case DOWNLOAD_FILE -> {
                                if (controllerName.equals("ChatPanels")) {
                                    try {
                                        java.lang.reflect.Method method = controller.getClass().getMethod("handleIncomingFileResponse", org.example.protocol.Message.class);
                                        method.invoke(controller, responseMessage);
                                    } catch (Exception e) {
                                        System.err.println("[UI_BRIDGE ERROR] Failed to invoke handleIncomingFileResponse: " + e.getMessage());
                                    }
                                }
                            }

                            case GET_GROUP_MEMBERS -> {
                                if (controllerName.equals("ChatPanels")) {
                                    Method method = controller.getClass().getMethod("showGroupMembersWindow", String.class);
                                    method.invoke(controller, responseMessage.getText());
                                }
                            }

                            case GET_ADMIN_STATS, GET_ALL_USERS, ADMIN_ACTION_ROLE, ADMIN_ACTION_BLOCK -> {
                                if (controllerName.equals("AdminPage")) {
                                    Method method = controller.getClass().getMethod("handleAdminResponse", Message.class);
                                    method.invoke(controller, responseMessage);
                                }
                            }
                            case NEW_LOG -> {
                                if (controllerName.equals("AdminPage")) {
                                    try {
                                        Method method = controller.getClass().getMethod("handleAdminResponse", Message.class);
                                        method.invoke(controller, responseMessage);
                                    } catch (Exception e) {
                                        System.err.println("[UI_BRIDGE ERROR] Failed to invoke NEW_LOG: " + e.getMessage());
                                    }
                                }
                            }

                            default -> System.out.println("[UI_BRIDGE INFO] Unrouted command text: " + responseMessage.getText());
                        }
                    } catch (NoSuchMethodException e) {
                        System.out.println("[UI_BRIDGE NOTICE] Missing handler method in " + controllerName + " for " + responseMessage.getCommandType());
                    } catch (Exception e) {
                        System.err.println("[UI_BRIDGE ERROR] Dynamic invocation failed: " + e.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("[RECEIVER_THREAD ERROR] Connection lost: " + e.getMessage());
        }
    }
}