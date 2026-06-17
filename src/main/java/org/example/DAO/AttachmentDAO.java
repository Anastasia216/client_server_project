package org.example.DAO;
import org.example.models.Attachment;

import java.util.List;

public interface AttachmentDAO {
    Attachment save(Attachment attachment);
    List<Attachment> findByMessageId(long messageId);
    void delete(long id);
}
