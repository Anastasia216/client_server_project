package org.example.protocol;

public enum CommandType {
    LOGIN(1001),
    REGISTER(1002),
    SEND_MESSAGE(2001),
    GET_CHAT_HISTORY(2002),
    SEND_FILE(2003),
    DOWNLOAD_FILE(2004),
    SEARCH(2010),

    CREATE_CHAT(3001),
    ADD_USER_TO_CHAT(3002),
    GET_CHATS(3003),
    CREATE_GROUP(3004),
    GET_CONTACTS(3005),
    BLOCK_USER(4001),
    DELETE_CHAT(3006),
    GET_GROUP_MEMBERS(3007),
    PROMOTE_TO_ADMIN(3008),
    RENAME_CHAT(3009),
    GET_ALL_USERS(3011),
    GET_ADMIN_STATS(3040),
    ADMIN_ACTION_ROLE(3041),
    ADMIN_ACTION_BLOCK(3042),
    LEAVE_CHAT(3010),
    ADD_GROUP_MEMBER(3020),
    REMOVE_GROUP_MEMBER(3030),

    GET_PROFILE(3101),
    UPDATE_PROFILE(3102),

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