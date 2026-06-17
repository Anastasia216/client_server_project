package org.example.network;

import org.example.protocol.Decoder;
import org.example.protocol.Encoder;
import org.example.protocol.Packet;
import org.example.protocol.Message;

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
                byte[] headerBase = in.readNBytes(14);
                if (headerBase.length < 14) {
                    break;
                }

                int wLen = ByteBuffer.wrap(headerBase, 10, 4).getInt();

                int restSize = 2 + wLen + 2;
                byte[] restBytes = in.readNBytes(restSize);
                if (restBytes.length < restSize) {
                    break;
                }

                byte[] fullPacketData = new byte[14 + restSize];
                System.arraycopy(headerBase, 0, fullPacketData, 0, 14);
                System.arraycopy(restBytes, 0, fullPacketData, 14, restSize);

                Packet requestPacket = Decoder.decode(fullPacketData);
                System.out.println("[HANDLER] Received packet ID: " + requestPacket.bPktId);

                if (authorizedUserId == -1 &&
                        requestPacket.bMsq.cType != CommandType.LOGIN.getId() &&
                        requestPacket.bMsq.cType != CommandType.REGISTER.getId()) {
                    Message accessDenied = new Message(100, 0, "ERROR:NOT_AUTHORIZED");
                    Packet errorPacket = new Packet((byte) 0, requestPacket.bPktId, accessDenied);
                    out.write(Encoder.encode(errorPacket));
                    out.flush();
                    continue;
                }

                Message responseMessage = processor.process(requestPacket.bMsq);

                if (requestPacket.bMsq.cType == CommandType.LOGIN.getId() && responseMessage.message.startsWith("SUCCESS")) {
                    this.authorizedUserId = responseMessage.bUserId;
                    System.out.println("[HANDLER] Socket successfully assigned to User ID: " + authorizedUserId);
                }

                Packet responsePacket = new Packet((byte) 0, requestPacket.bPktId, responseMessage);
                byte[] responseBytes = Encoder.encode(responsePacket);
                out.write(responseBytes);
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
            e.printStackTrace();
        }
    }
}