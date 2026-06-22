package org.example.network;

import org.example.protocol.CommandType;
import org.example.protocol.Message;
import org.example.protocol.MessagePacket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class NetworkClient {
    private static NetworkClient instance;
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 5001;

    private Socket socket;
    private OutputStream out;
    private InputStream in;
    private boolean connected = false;
    private Object activeController;

    private NetworkClient() {}

    public static synchronized NetworkClient getInstance() {
        if (instance == null) {
            instance = new NetworkClient();
        }
        return instance;
    }

    public void connect() throws IOException {
        if (connected) return;
        System.out.println("[NETWORK_CLIENT] Connecting to server at " + HOST + ":" + PORT + "...");
        this.socket = new Socket(HOST, PORT);
        this.out = socket.getOutputStream();
        this.in = socket.getInputStream();
        this.connected = true;
        System.out.println("[NETWORK_CLIENT] Connected to server successfully.");

        // Запуск фонового потоку-слухача
        Thread receiverThread = new Thread(new NetworkReceiverThread());
        receiverThread.setDaemon(true);
        receiverThread.start();
        System.out.println("[NETWORK_CLIENT] Background receiver thread sparked successfully.");
    }

    public synchronized void sendPacket(MessagePacket packet) {
        try {
            if (out != null && !socket.isClosed()) {
                out.write(packet.toBytes());
                out.flush();
                System.out.println("[NETWORK_CLIENT] Sent packet type: " + packet.getMessage().getCommandType());
            }
        } catch (IOException e) {
            System.err.println("[NETWORK_CLIENT ERROR] Send failed: " + e.getMessage());
        }
    }

    public void setActiveController(Object controller) {
        this.activeController = controller;
        if (controller != null) {
            System.out.println("[NETWORK_CLIENT] Switched bridge focus to UI Controller: " + controller.getClass().getSimpleName());
        }
    }

    public Object getActiveController() {
        return activeController;
    }

    public void sendLoginRequest(String username, String password) {
        String rawData = username + ";" + password;
        Message loginMessage = new Message(CommandType.LOGIN, 0, rawData);
        MessagePacket packet = new MessagePacket((byte) 1, System.currentTimeMillis(), loginMessage);
        sendPacket(packet);
    }

    public void sendRegisterRequest(String username, String email, String password) {
        String rawData = username + ";" + email + ";" + password;
        Message registerMessage = new Message(CommandType.REGISTER, 0, rawData);
        MessagePacket packet = new MessagePacket((byte) 1, System.currentTimeMillis(), registerMessage);
        sendPacket(packet);
    }

    public void sendTextMessage(int chatId, int receiverId, String text, int currentUserId) {
        String rawData = chatId + ";" + receiverId + ";" + text;
        Message messageMsg = new Message(CommandType.SEND_MESSAGE, currentUserId, rawData);
        MessagePacket packet = new MessagePacket((byte) 1, System.currentTimeMillis(), messageMsg);
        sendPacket(packet);
    }

    public void sendFileRequest(int chatId, int receiverId, String fileName, byte[] fileBytes, int currentUserId) {
        String base64Data = java.util.Base64.getEncoder().encodeToString(fileBytes);
        String rawData = chatId + ";" + receiverId + ";" + fileName + ";" + base64Data;
        Message fileMessage = new Message(CommandType.SEND_FILE, currentUserId, rawData);
        MessagePacket packet = new MessagePacket((byte) 1, System.currentTimeMillis(), fileMessage);
        sendPacket(packet);
    }

    public void sendGetHistoryRequest(int chatId, int currentUserId) {
        String rawData = String.valueOf(chatId);
        Message historyMessage = new Message(CommandType.GET_CHAT_HISTORY, currentUserId, rawData);
        MessagePacket packet = new MessagePacket((byte) 1, System.currentTimeMillis(), historyMessage);
        sendPacket(packet);
    }

    public void sendBlockUserRequest(int userIdToBlock, int adminUserId) {
        String rawData = String.valueOf(userIdToBlock);
        Message blockMessage = new Message(CommandType.BLOCK_USER, adminUserId, rawData);
        MessagePacket packet = new MessagePacket((byte) 1, System.currentTimeMillis(), blockMessage);
        sendPacket(packet);
    }

    public InputStream getInputStream() { return in; }
    public Socket getSocket() { return socket; }
    public boolean isConnected() { return connected; }

    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                connected = false;
                System.out.println("[NETWORK_CLIENT] Disconnected.");
            }
        } catch (IOException e) { e.printStackTrace(); }
    }
}