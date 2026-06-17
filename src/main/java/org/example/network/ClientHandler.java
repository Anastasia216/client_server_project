package org.example.network;

import org.example.protocol.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private static final Processor processor = new Processor();
    private int authorizedUserId = -1;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream()
        ) {
            while (!socket.isClosed()) {
                // 1. Читаємо 16 байтів заголовка, як прописано в MessagePacket
                byte[] headerBase = in.readNBytes(MessagePacket.HEADER_SIZE);
                if (headerBase.length < MessagePacket.HEADER_SIZE) {
                    break;
                }

                // wLen лежить у буфері зсувом з 10 по 14 байт
                int wLen = ByteBuffer.wrap(headerBase, 10, 4).getInt();

                // Читаємо payload + 2 байти CRC корисного навантаження
                int restSize = wLen + 2;
                byte[] restBytes = in.readNBytes(restSize);
                if (restBytes.length < restSize) {
                    break;
                }

                // 2. Декодуємо пакет через фабричний метод з MessagePacket
                MessagePacket requestPacket = MessagePacket.fromBytes(headerBase, restBytes);
                System.out.println("[HANDLER] Received packet ID: " + requestPacket.getMessageNum());

                Message messageObj = requestPacket.getMessage();

                // 3. Перевірка авторизації через нові геттери
                if (authorizedUserId == -1 &&
                        messageObj.getCommandType() != CommandType.LOGIN &&
                        messageObj.getCommandType() != CommandType.REGISTER) {

                    Message accessDenied = new Message(CommandType.STATUS_ERROR, 0, "ERROR:NOT_AUTHORIZED");
                    MessagePacket errorPacket = new MessagePacket((byte) 0, requestPacket.getMessageNum(), accessDenied);
                    out.write(errorPacket.toBytes());
                    out.flush();
                    continue;
                }

                // 4. Передаємо логіку в процесор
                Message responseMessage = processor.process(messageObj);

                // Якщо логін успішний — запам'ятовуємо користувача для поточного сокета
                if (messageObj.getCommandType() == CommandType.LOGIN && responseMessage.getText().startsWith("SUCCESS")) {
                    this.authorizedUserId = responseMessage.getUserId();
                    System.out.println("[HANDLER] Socket successfully assigned to User ID: " + authorizedUserId);
                }

                // 5. Формуємо відповідь через новий MessagePacket і відправляємо назад
                MessagePacket responsePacket = new MessagePacket((byte) 0, requestPacket.getMessageNum(), responseMessage);
                out.write(responsePacket.toBytes());
                out.flush();
            }
        } catch (Exception e) {
            System.out.println("[HANDLER] Error or client disconnected: " + e.getMessage());
        } finally {
            closeSocket();
        }
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