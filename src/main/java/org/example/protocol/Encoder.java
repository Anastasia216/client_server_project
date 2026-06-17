package org.example.protocol;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Encoder {
    private static final String SECRET_KEY = "1234567890123456";
    public static byte[] encode(Packet packet) throws Exception {
        byte bMagic = 0x13;
        ByteBuffer messageBuffer = ByteBuffer.allocate(packet.bMsq.message.getBytes().length +8);
        messageBuffer.putInt(packet.bMsq.cType);
        messageBuffer.putInt(packet.bMsq.bUserId);
        messageBuffer.put(packet.bMsq.message.getBytes());
        messageBuffer.flip();

        Cipher cipher = Cipher.getInstance("AES");
        SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedMessage = cipher.doFinal(messageBuffer.array());

        int wLen = encryptedMessage.length;
        ByteBuffer headerBuffer = ByteBuffer.allocate(14);
        headerBuffer.put(bMagic);
        headerBuffer.put(packet.bSrc);
        headerBuffer.putLong(packet.bPktId);
        headerBuffer.putInt(wLen);

        ByteBuffer fullBuffer = ByteBuffer.allocate(1024);
        fullBuffer.put(headerBuffer.array());

        short headerCrc = Crc16.calculateCrc(headerBuffer.array());
        fullBuffer.putShort(headerCrc);

        fullBuffer.put(encryptedMessage);
        short messageCrc = Crc16.calculateCrc(encryptedMessage);
        fullBuffer.putShort(messageCrc);
        return fullBuffer.array();
    }
}
