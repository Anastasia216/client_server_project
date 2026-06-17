package org.example.protocol;

public class Packet {

    public byte bSrc;
    public long bPktId;
    public Message bMsq;

    public Packet() {
    }

    public Packet(byte bSrc, long bPktId, Message bMsq) {
        this.bSrc = bSrc;
        this.bPktId = bPktId;
        this.bMsq = bMsq;
    }
}
