package org.example.network;

import org.example.protocol.CommandType;
import org.example.protocol.Message;
import org.example.protocol.MessagePacket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public class NetworkClient {
    private static NetworkClient instance;
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 5001;
    private Socket socket;
    private OutputStream out;
    private InputStream in;
    private boolean connected = false;

    private NetworkClient() {}

    public static synchronized NetworkClient getInstance() {
        if (instance == null) {
            instance = new NetworkClient();
        }
        return instance;
    }

    public void connect() throws IOException {
        if (connected) return;
        this.socket = new Socket(HOST, PORT);
        this.out = socket.getOutputStream();
        this.in = socket.getInputStream();
        this.connected = true;
        System.out.println("[NETWORK_CLIENT] Connected to server.");
    }

    public synchronized void sendPacket(MessagePacket packet) {
        try {
            if (out != null && !socket.isClosed()) {
                out.write(packet.toBytes());
                out.flush();
                System.out.println("[NETWORK_CLIENT] Sent packet: " + packet.getMessage().getCommandType());
            }
        } catch (IOException e) {
            System.err.println("[NETWORK_CLIENT ERROR] Send failed: " + e.getMessage());
        }
    }

    public void sendLoginRequest(String username, String password) {
        String rawData = username + ";" + password;
        Message loginMessage = new Message(CommandType.LOGIN, 0, rawData);
        MessagePacket packet = new MessagePacket((byte) 1, System.currentTimeMillis(), loginMessage);
        sendPacket(packet);
    }

    public void sendRegisterRequest(String username, String phone, String password) {
        String rawData = username + ";" + phone + ";" + password;
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

    public void sendFileMessage(int chatId, String fileName, String base64Data) {
        String rawData = chatId + ";" + fileName + ";" + base64Data;
        Message messageMsg = new Message(CommandType.SEND_FILE, 0, rawData);
        MessagePacket packet = new MessagePacket((byte) 1, System.currentTimeMillis(), messageMsg);
        sendPacket(packet);
    }

    public void sendDownloadFileRequest(long messageId) {
        Message req = new Message(CommandType.DOWNLOAD_FILE, 0, String.valueOf(messageId));
        MessagePacket packet = new MessagePacket((byte) 1, System.currentTimeMillis(), req);
        sendPacket(packet);
    }

    public void sendSearchRequest(String query) {
        Message searchMessage = new Message(CommandType.SEARCH, 0, query);
        MessagePacket packet = new MessagePacket((byte) 1, System.currentTimeMillis(), searchMessage);
        sendPacket(packet);
    }

    public void sendGetChatsRequest() {
        Message requestMessage = new Message(CommandType.GET_CHATS, 0, "REQUEST_CHATS");
        MessagePacket packet = new MessagePacket((byte) 1, System.currentTimeMillis(), requestMessage);
        sendPacket(packet);
    }

    public void sendGetChatHistoryRequest(String chatName) {
        Message req = new Message(CommandType.GET_CHAT_HISTORY, 0, chatName);
        MessagePacket packet = new MessagePacket((byte) 1, System.currentTimeMillis(), req);
        sendPacket(packet);
    }

    public void sendGetContactsRequest() {
        Message req = new Message(CommandType.GET_CONTACTS, 0, "REQUEST_CONTACTS");
        MessagePacket packet = new MessagePacket((byte) 1, System.currentTimeMillis(), req);
        sendPacket(packet);
    }

    public void sendCreateGroupRequest(String groupName, java.util.List<Integer> memberIds) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < memberIds.size(); i++) {
            sb.append(memberIds.get(i));
            if (i < memberIds.size() - 1) sb.append(",");
        }
        String rawData = groupName + ";" + sb.toString();

        Message req = new Message(CommandType.CREATE_GROUP, 0, rawData);
        MessagePacket packet = new MessagePacket((byte) 1, System.currentTimeMillis(), req);
        sendPacket(packet);
    }

    public InputStream getInputStream() {
        return in;
    }

    public Socket getSocket() {
        return socket;
    }

    public boolean isConnected() {
        return connected;
    }

    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                connected = false;
                System.out.println("[NETWORK_CLIENT] Disconnected.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public MessagePacket receivePacket() throws IOException {
        byte[] headerBase = in.readNBytes(MessagePacket.HEADER_SIZE);
        if (headerBase.length < MessagePacket.HEADER_SIZE) {
            throw new IOException("Connection closed by server");
        }
        int wLen = ByteBuffer.wrap(headerBase, 10, 4).getInt();
        int restSize = wLen + 2;
        byte[] restBytes = in.readNBytes(restSize);
        if (restBytes.length < restSize) {
            throw new IOException("Incomplete packet data");
        }
        return MessagePacket.fromBytes(headerBase, restBytes);
    }
}