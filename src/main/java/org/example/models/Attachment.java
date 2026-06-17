package org.example.models;

import java.time.LocalDateTime;

public class Attachment {
    private long id;
    private long messageId;
    private String fileName;
    private String filePath;
    private long fileSize;
    private LocalDateTime uploadTime;
    public Attachment() {
    }

    public Attachment(long id, long messageId, String fileName, String filePath, long fileSize, LocalDateTime uploadTime) {
        this.id = id;
        this.messageId = messageId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.uploadTime = uploadTime;
    }

    public long getId() {

        return id;
    }

    public void setId(long id) {

        this.id = id;
    }

    public long getMessageId() {
        return messageId;
    }

    public void setMessageId(long messageId) {

        this.messageId = messageId;
    }

    public String getFileName() {

        return fileName;
    }

    public void setFileName(String fileName) {

        this.fileName = fileName;
    }

    public String getFilePath() {

        return filePath;
    }

    public void setFilePath(String filePath) {

        this.filePath = filePath;
    }

    public long getFileSize() {

        return fileSize;
    }

    public void setFileSize(long fileSize) {

        this.fileSize = fileSize;
    }

    public LocalDateTime getUploadTime() {

        return uploadTime;
    }

    public void setUploadTime(LocalDateTime uploadTime) {

        this.uploadTime = uploadTime;
    }
}