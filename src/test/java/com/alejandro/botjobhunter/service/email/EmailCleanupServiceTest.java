package com.alejandro.botjobhunter.service.email;

import com.alejandro.botjobhunter.config.EmailCleanupProperties;
import com.alejandro.botjobhunter.models.enums.EmailCleanupStrategy;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Store;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailCleanupServiceTest {

    @Test
    void cleanupShouldReturnImmediatelyWhenStrategyIsNone() throws Exception {
        EmailCleanupService service = new EmailCleanupService(
                new EmailCleanupProperties(EmailCleanupStrategy.NONE, "BotJobHunter/Processed")
        );
        Message message = mock(Message.class);

        service.cleanup(message);

        verify(message, never()).setFlag(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void cleanupShouldMarkMessageDeletedWhenStrategyIsDelete() throws Exception {
        EmailCleanupService service = new EmailCleanupService(
                new EmailCleanupProperties(EmailCleanupStrategy.DELETE, "BotJobHunter/Processed")
        );
        Message message = mock(Message.class);

        service.cleanup(message);

        verify(message).setFlag(Flags.Flag.DELETED, true);
    }

    @Test
    void cleanupShouldMarkSeenAndCopyToLabelFolderWhenStrategyIsArchive() throws Exception {
        EmailCleanupService service = new EmailCleanupService(
                new EmailCleanupProperties(EmailCleanupStrategy.ARCHIVE, "BotJobHunter/Processed")
        );
        Message message = mock(Message.class);
        Folder sourceFolder = mock(Folder.class);
        Store store = mock(Store.class);
        Folder targetFolder = mock(Folder.class);

        when(message.getFolder()).thenReturn(sourceFolder);
        when(sourceFolder.getStore()).thenReturn(store);
        when(store.getFolder("BotJobHunter/Processed")).thenReturn(targetFolder);
        when(targetFolder.exists()).thenReturn(false);

        service.cleanup(message);

        verify(message).setFlag(Flags.Flag.SEEN, true);
        verify(targetFolder).create(Folder.HOLDS_MESSAGES);
        verify(sourceFolder).copyMessages(new Message[]{message}, targetFolder);
    }
}
