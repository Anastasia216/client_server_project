package org.example.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class MessagePacket {
    public static final byte MAGIC = 0x13;
    public static final int HEADER_SIZE = 16;

    private byte uniqueIdentifier;
    private long messageNum;
    private Message message;

    public MessagePacket() {}

    public MessagePacket(byte uniqueIdentifier, long messageNum, Message message) {
        this.uniqueIdentifier = uniqueIdentifier;
        this.messageNum = messageNum;
        this.message = message;
    }

    public byte[] toBytes() {
        byte[] stringBytes = (message != null && message.getText() != null)
                ? message.getText().getBytes(StandardCharsets.UTF_8) : new byte[0];

        int wLen = 4 + 4 + stringBytes.length;
        int totalSize = HEADER_SIZE + wLen + 2;

        ByteBuffer buffer = ByteBuffer.allocate(totalSize);

        buffer.put(MAGIC);
        buffer.put(uniqueIdentifier);
        buffer.putLong(messageNum);
        buffer.putInt(wLen);

        short headerCrc = calculateCRC16(buffer.array(), 0, 14);
        buffer.putShort(headerCrc);

        int payloadOffset = buffer.position();
        buffer.putInt(message.getCommandType().getId());
        buffer.putInt(message.getUserId());
        buffer.put(stringBytes);

        short payloadCrc = calculateCRC16(buffer.array(), payloadOffset, wLen);
        buffer.putShort(payloadCrc);

        return buffer.array();
    }

    public static MessagePacket fromBytes(byte[] headerBytes, byte[] payloadBytes) {
        if (headerBytes.length != HEADER_SIZE) throw new IllegalArgumentException("Invalid header size");

        ByteBuffer headerBuf = ByteBuffer.wrap(headerBytes);
        byte magic = headerBuf.get();
        if (magic != MAGIC) throw new IllegalArgumentException("Incorrect magic byte");

        byte src = headerBuf.get();
        long pktId = headerBuf.getLong();
        int wLen = headerBuf.getInt();
        short headerCrc = headerBuf.getShort();

        if (headerCrc != calculateCRC16(headerBytes, 0, 14)) {
            throw new SecurityException("CRC16 of header does not match");
        }

        if (payloadBytes.length != wLen + 2) throw new IllegalArgumentException("Invalid payload length");
        short expectedPayloadCrc = calculateCRC16(payloadBytes, 0, wLen);
        ByteBuffer payloadBuf = ByteBuffer.wrap(payloadBytes);
        short actualPayloadCrc = payloadBuf.getShort(wLen);
        if (expectedPayloadCrc != actualPayloadCrc) {
            throw new SecurityException("CRC16 of message payload does not match");
        }

        int commandId = payloadBuf.getInt();
        int userId = payloadBuf.getInt();
        int textLength = wLen - 8;
        byte[] textBytes = new byte[textLength];
        payloadBuf.get(textBytes);
        String msgStr = new String(textBytes, StandardCharsets.UTF_8);

        CommandType cType = CommandType.fromId(commandId);
        Message msgObj = new Message(cType, userId, msgStr);

        return new MessagePacket(src, pktId, msgObj);
    }

    private static short calculateCRC16(byte[] bytes, int offset, int length) {
        int crc = 0xFFFF;
        for (int i = offset; i < offset + length; i++) {
            crc ^= (bytes[i] & 0xFF);
            for (int j = 0; j < 8; j++) {
                if ((crc & 0x0001) != 0) {
                    crc = (crc >>> 1) ^ 0xA001;
                } else {
                    crc >>>= 1;
                }
            }
        }
        return (short) crc;
    }

    public byte getUniqueIdentifier() { return uniqueIdentifier; }
    public void setUniqueIdentifier(byte uniqueIdentifier) { this.uniqueIdentifier = uniqueIdentifier; }
    public long getMessageNum() { return messageNum; }
    public void setMessageNum(long messageNum) { this.messageNum = messageNum; }
    public Message getMessage() { return message; }
    public void setMessage(Message message) { this.message = message; }
}