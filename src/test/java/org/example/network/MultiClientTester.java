package org.example.network;

import org.example.protocol.CommandType;
import org.example.protocol.Message;
import org.example.protocol.MessagePacket;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiClientTester {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 5001;

    private static final String[] TEST_USERS = {
            "ivan;123",
            "olya;123",
            "misha;123",

    };

    public static void main(String[] args) {
        System.out.println("[TESTER] Starting multi-client concurrency test...");
        ExecutorService executor = Executors.newFixedThreadPool(TEST_USERS.length);

        for (String credentials : TEST_USERS) {
            executor.submit(() -> simulateUser(credentials));
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {}
        }

        System.out.println("[TESTER] All virtual clients are running. Press Ctrl+C in terminal to stop.");
    }

    private static void simulateUser(String credentials) {
        String[] parts = credentials.split(";");
        String username = parts[0];
        String password = parts[1];

        System.out.println("[VIRTUAL CLIENT] (" + username + ") Connecting to server...");

        try (Socket socket = new Socket(HOST, PORT);
             OutputStream out = socket.getOutputStream();
             InputStream in = socket.getInputStream()) {

            System.out.println("[VIRTUAL CLIENT] (" + username + ") Connected! Sending LOGIN request...");

            String rawData = username + ";" + password;
            Message loginMessage = new Message(CommandType.LOGIN, 0, rawData);
            MessagePacket packet = new MessagePacket((byte) 1, System.currentTimeMillis(), loginMessage);

            out.write(packet.toBytes());
            out.flush();

            byte[] headerBase = in.readNBytes(MessagePacket.HEADER_SIZE);
            if (headerBase.length == MessagePacket.HEADER_SIZE) {
                int wLen = ByteBuffer.wrap(headerBase, 10, 4).getInt();
                int restSize = wLen + 2;
                byte[] restBytes = in.readNBytes(restSize); // Читаємо залишок пакету

                MessagePacket responsePacket = MessagePacket.fromBytes(headerBase, restBytes);
                System.out.println("[VIRTUAL CLIENT] (" + username + ") Response from server: " + responsePacket.getMessage().getText());
            }

            System.out.println("[VIRTUAL CLIENT] (" + username + ") is now holding the connection (ONLINE state)...");

            while (!socket.isClosed()) {
                Thread.sleep(5000);
            }

        } catch (Exception e) {
            System.err.println("[VIRTUAL CLIENT ERROR] (" + username + ") Connection lost: " + e.getMessage());
        }
    }
}
