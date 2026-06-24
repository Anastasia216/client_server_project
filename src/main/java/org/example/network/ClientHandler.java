package org.example.network;

import org.example.protocol.*;
import org.example.service.AuthService;
import org.example.DAO.impl.SQLiteUserDAO;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private static final Processor processor = new Processor();
    private final AuthService authService = new AuthService(new SQLiteUserDAO());
    private int authorizedUserId = -1;
    private OutputStream out;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }
    public synchronized void sendPacket(MessagePacket packet) {
        try {
            if (out != null && !socket.isClosed()) {
                byte[] bytesToSend = packet.toBytes();
                out.write(bytesToSend);
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("[HANDLER ERROR] Failed to send packet to user ID " + authorizedUserId + ": " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try (
                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream()
        ) {
            this.out = out;
            while (!socket.isClosed()) {
                byte[] headerBase = in.readNBytes(MessagePacket.HEADER_SIZE);
                if (headerBase.length < MessagePacket.HEADER_SIZE) {
                    break;
                }

                int wLen = ByteBuffer.wrap(headerBase, 10, 4).getInt();
                int restSize = wLen + 2;
                byte[] restBytes = in.readNBytes(restSize);
                if (restBytes.length < restSize) {
                    break;
                }

                MessagePacket requestPacket = MessagePacket.fromBytes(headerBase, restBytes);
                System.out.println("[HANDLER] Received packet ID: " + requestPacket.getMessageNum());

                Message messageObj = requestPacket.getMessage();

                if (authorizedUserId == -1 &&
                        messageObj.getCommandType() != CommandType.LOGIN &&
                        messageObj.getCommandType() != CommandType.REGISTER) {

                    Message accessDenied = new Message(CommandType.STATUS_ERROR, 0, "ERROR:NOT_AUTHORIZED");
                    MessagePacket errorPacket = new MessagePacket((byte) 0, requestPacket.getMessageNum(), accessDenied);
                    out.write(errorPacket.toBytes());
                    out.flush();
                    continue;
                }

                Message responseMessage = processor.process(messageObj);

                if (messageObj.getCommandType() == CommandType.LOGIN && responseMessage.getText().startsWith("SUCCESS")) {
                    this.authorizedUserId = responseMessage.getUserId();
                    System.out.println("[HANDLER] Socket successfully assigned to User ID: " + authorizedUserId);
                    ClientRegistry.addClient(this.authorizedUserId, this);
                    authService.updateStatus(authorizedUserId, "ONLINE");
                    ClientRegistry.broadcastUserStatusChange(authorizedUserId, "ONLINE");
                }

                MessagePacket responsePacket = new MessagePacket((byte) 0, requestPacket.getMessageNum(), responseMessage);
                sendPacket(responsePacket);
            }
        } catch (IOException e) {
            System.err.println("[HANDLER INFO] Connection closed or reset for user ID " + authorizedUserId);
        } finally {
            if (authorizedUserId != -1) {
                ClientRegistry.removeClient(authorizedUserId);
                authService.updateStatus(authorizedUserId, "OFFLINE");
                ClientRegistry.broadcastUserStatusChange(authorizedUserId, "OFFLINE");
            }
            closeSocket();
        }
    }

    public void forceDisconnect() {
        Message banMessage = new Message(CommandType.STATUS_ERROR, 0, "ERROR:YOU_ARE_BLOCKED_BY_ADMIN");
        sendPacket(new MessagePacket((byte) 0, 0, banMessage));
        closeSocket();
    }

    private void closeSocket() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("[HANDLER] Client socket closed successfully.");
            }
        } catch (IOException e) {
            System.err.println("[HANDLER ERROR] Failed to close socket: " + e.getMessage());
        }
    }

}