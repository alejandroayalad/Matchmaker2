package com.alejandro.botjobhunter.service.email;

import com.alejandro.botjobhunter.config.EmailCleanupProperties;
import com.alejandro.botjobhunter.models.enums.EmailCleanupStrategy;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "mail", name = "enabled", havingValue = "true")
public class EmailCleanupService {

    private final EmailCleanupProperties cleanupProperties;

    public EmailCleanupService(EmailCleanupProperties cleanupProperties) {
        this.cleanupProperties = cleanupProperties;
    }

    public void cleanup(Message message) throws MessagingException {
        EmailCleanupStrategy strategy = cleanupProperties.strategy();
        if (strategy == null || strategy == EmailCleanupStrategy.NONE) {
            return;
        }

        if (strategy == EmailCleanupStrategy.DELETE) {
            message.setFlag(Flags.Flag.DELETED, true);
            return;
        }

        if (strategy == EmailCleanupStrategy.ARCHIVE) {
            archive(message);
        }
    }

    private void archive(Message message) throws MessagingException {
        String label = cleanupProperties.label();
        if (label == null || label.isBlank()) {
            throw new IllegalStateException("mail.cleanup.label is required when archive cleanup is enabled.");
        }

        Folder sourceFolder = message.getFolder();
        Folder targetFolder = sourceFolder.getStore().getFolder(label.trim());

        if (!targetFolder.exists()) {
            targetFolder.create(Folder.HOLDS_MESSAGES);
        }

        message.setFlag(Flags.Flag.SEEN, true);
        sourceFolder.copyMessages(new Message[]{message}, targetFolder);
    }
}
