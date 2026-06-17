package org.example.protocol;

import java.nio.ByteBuffer;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Decoder {
    private static final String SECRET_KEY = "1234567890123456";
    public static Packet decode(byte[] bytes) throws Exception{
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        byte bMagic = buffer.get();
        if (bMagic != 0x13){
            throw new RuntimeException("Invalid Magic Byte");
        }
        byte bSrc = buffer.get();
        long bPktId = buffer.getLong();
        int wLen = buffer.getInt();
        short receivedHeaderCrc = buffer.getShort();
        byte[] headerBytes = Arrays.copyOfRange(bytes, 0, 14);
        short calculatesHeaderCrc = Crc16.calculateCrc(headerBytes);
        if (receivedHeaderCrc != calculatesHeaderCrc){
            throw new RuntimeException("Invalid Header Crc");
        }

        byte[] messageBytes = new byte[wLen];
        buffer.get(messageBytes);
        short receivedMessageCrc = buffer.getShort();
        short calculatedMessageCrc = Crc16.calculateCrc(messageBytes);
        if (receivedMessageCrc != calculatedMessageCrc){
            throw new RuntimeException("Invalid Message Crc");
        }

        Cipher cipher = Cipher.getInstance("AES");
        SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedBytes = cipher.doFinal(messageBytes);
        ByteBuffer messageBuffer = ByteBuffer.wrap(decryptedBytes);
        int cType = messageBuffer.getInt();
        int bUserId = messageBuffer.getInt();
        byte[] textBytes = new byte[decryptedBytes.length - 8];
        messageBuffer.get(textBytes);
        String message = new String(textBytes);

        Message msg = new Message(cType, bUserId, message);
        return new Packet(bSrc, bPktId, msg);
    }
}