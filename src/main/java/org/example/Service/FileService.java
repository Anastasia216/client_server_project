package org.example.Service;

import org.example.DAO.AttachmentDAO;
import org.example.models.Attachment;

import java.util.List;

public class FileService {
    private final AttachmentDAO attachmentDAO;

    public FileService(AttachmentDAO attachmentDAO) {
        this.attachmentDAO = attachmentDAO;
    }

    public Attachment registerAttachment(long messageId, String fileName, String filePath, long fileSize) {
        Attachment attachment = new Attachment();
        attachment.setMessageId(messageId);
        attachment.setFileName(fileName);
        attachment.setFilePath(filePath);
        attachment.setFileSize(fileSize);

        return attachmentDAO.save(attachment);
    }

    public List<Attachment> getFilesForMessage(long messageId) {
        return attachmentDAO.findByMessageId(messageId);
    }

    public void deleteFileRecord(long attachmentId) {
        attachmentDAO.delete(attachmentId);
    }
}