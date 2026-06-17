package org.example.protocol;

public class Message {
    private CommandType commandType;
    private int userId;
    private String text;

    public Message() {}

    public Message(CommandType commandType, int userId, String text) {
        this.commandType = commandType;
        this.userId = userId;
        this.text = text;
    }

    public CommandType getCommandType() {
        return commandType;
    }
    public void setCommandType(CommandType commandType) {
        this.commandType = commandType;
    }
    public int getUserId() {
        return userId;
    }
    public void setUserId(int userId) {
        this.userId = userId;
    }
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
}