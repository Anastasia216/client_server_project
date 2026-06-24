package org.example.network;

import org.example.protocol.CommandType;
import org.example.protocol.Message;
import org.example.protocol.MessagePacket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Base64;
import java.util.List;

public class NetworkClient {
    private static NetworkClient instance;
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 5001;

    private Socket socket;
    private OutputStream out;
    private InputStream in;
    private boolean connected = false;
    private Object activeController;

    private int myUserId = -1;
    private String myUsername = "";

    private NetworkClient() {}

    public static synchronized NetworkClient getInstance() {
        if (instance == null) {
            instance = new NetworkClient();
        }
        return instance;
    }

    private String myRole = "USER";
    public String getMyRole() { return myRole; }
    public void setMyRole(String myRole) { this.myRole = myRole; }

    public int getMyUserId() { return myUserId; }
    public void setMyUserId(int myUserId) { this.myUserId = myUserId; }

    public String getMyUsername() { return myUsername; }
    public void setMyUsername(String myUsername) { this.myUsername = myUsername; }

    public void connect() throws IOException {
        if (connected) return;
        System.out.println("[NETWORK_CLIENT] Connecting to server at " + HOST + ":" + PORT + "...");
        this.socket = new Socket(HOST, PORT);
        this.out = socket.getOutputStream();
        this.in = socket.getInputStream();
        this.connected = true;
        System.out.println("[NETWORK_CLIENT] Connected to server successfully.");

        Thread receiverThread = new Thread(new NetworkReceiverThread());
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    public synchronized void sendPacket(MessagePacket packet) {
        try {
            if (out != null && !socket.isClosed()) {
                out.write(packet.toBytes());
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("[NETWORK_CLIENT ERROR] Send failed: " + e.getMessage());
        }
    }

    public void setActiveController(Object controller) { this.activeController = controller; }
    public Object getActiveController() { return activeController; }

    public void sendLoginRequest(String username, String password) {
        this.myUsername = username;
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

    public void sendTextMessage(int chatId, String text) {
        String rawData = chatId + ";0;" + text;
        Message messageMsg = new Message(CommandType.SEND_MESSAGE, myUserId, rawData);
        MessagePacket packet = new MessagePacket((byte) 1, System.currentTimeMillis(), messageMsg);
        sendPacket(packet);
    }

    public void sendGetChatsRequest() {
        Message requestMsg = new Message(CommandType.GET_CHATS, myUserId, "");
        MessagePacket packet = new MessagePacket((byte) 1, System.currentTimeMillis(), requestMsg);
        sendPacket(packet);
    }

    public void sendGetHistoryRequest(int chatId) {
        String rawData = String.valueOf(chatId);
        Message historyMessage = new Message(CommandType.GET_CHAT_HISTORY, myUserId, rawData);
        MessagePacket packet = new MessagePacket((byte) 1, System.currentTimeMillis(), historyMessage);
        sendPacket(packet);
    }

    public void sendSearchRequest(String usernameOrPhone) {
        Message searchMessage = new Message(CommandType.SEARCH, myUserId, usernameOrPhone);
        MessagePacket packet = new MessagePacket((byte) 1, System.currentTimeMillis(), searchMessage);
        sendPacket(packet);
    }

    public void sendGetContactsRequest() {
        Message contactsMessage = new Message(CommandType.GET_CONTACTS, myUserId, "");
        MessagePacket packet = new MessagePacket((byte) 1, System.currentTimeMillis(), contactsMessage);
        sendPacket(packet);
    }

    public void sendCreateGroupRequest(String groupName, List<Integer> memberIds) {
        StringBuilder sb = new StringBuilder(groupName);
        for (int id : memberIds) {
            sb.append(";").append(id);
        }
        Message groupMessage = new Message(CommandType.CREATE_CHAT, myUserId, sb.toString());
        MessagePacket packet = new MessagePacket((byte) 1, System.currentTimeMillis(), groupMessage);
        sendPacket(packet);
    }

    public void sendFileRequest(int chatId, String fileName, byte[] fileBytes) {
        String base64Data = Base64.getEncoder().encodeToString(fileBytes);
        String rawData = chatId + ";0;" + fileName + ";" + base64Data;
        Message fileMessage = new Message(CommandType.SEND_FILE, myUserId, rawData);
        MessagePacket packet = new MessagePacket((byte) 1, System.currentTimeMillis(), fileMessage);
        sendPacket(packet);
    }

    public void sendDeleteChatRequest(int chatId) {
        String rawData = String.valueOf(chatId);
        Message deleteMessage = new Message(CommandType.DELETE_CHAT, myUserId, rawData);
        MessagePacket packet = new MessagePacket((byte) 1, System.currentTimeMillis(), deleteMessage);
        sendPacket(packet);
    }

    public void sendGetGroupMembersRequest(int chatId) {
        String rawData = String.valueOf(chatId);
        Message msg = new Message(CommandType.GET_GROUP_MEMBERS, myUserId, rawData);
        MessagePacket packet = new MessagePacket((byte) 1, System.currentTimeMillis(), msg);
        sendPacket(packet);
    }

    public void sendRenameChatRequest(int chatId, String newName) {
        String rawData = chatId + ";" + newName;
        Message msg = new Message(CommandType.RENAME_CHAT, myUserId, rawData);
        MessagePacket packet = new MessagePacket((byte) 1, System.currentTimeMillis(), msg);
        sendPacket(packet);
    }

    public void sendPromoteToAdminRequest(int chatId, int targetUserId) {
        String rawData = chatId + ";" + targetUserId;
        Message msg = new Message(CommandType.PROMOTE_TO_ADMIN, myUserId, rawData);
        MessagePacket packet = new MessagePacket((byte) 1, System.currentTimeMillis(), msg);
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
                myUserId = -1;
                myUsername = "";
            }
        } catch (IOException e) { e.printStackTrace(); }
    }
}