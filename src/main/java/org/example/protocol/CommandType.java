package org.example.protocol;

public enum CommandType {
    LOGIN(1001),
    REGISTER(1002),
    SEND_MESSAGE(2001),
    GET_CHAT_HISTORY(2002),
    CREATE_CHAT(3001),
    ADD_USER_TO_CHAT(3002),
    BLOCK_USER(4001),
    STATUS_OK(200),
    STATUS_ERROR(400);

    private final int id;

    CommandType(int id) { this.id = id; }
    public int getId() { return id; }

    public static CommandType fromId(int id) {
        for (CommandType type : values()) {
            if (type.id == id) return type;
        }
        return null;
    }
}