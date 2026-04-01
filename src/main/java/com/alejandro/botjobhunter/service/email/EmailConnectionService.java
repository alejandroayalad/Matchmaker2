package com.alejandro.botjobhunter.service.email;

import com.alejandro.botjobhunter.config.ImapProperties;
import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import org.springframework.stereotype.Service;

@Service
public class EmailConnectionService {

    private final Session session;
    private final ImapProperties imapProperties;

    public EmailConnectionService(Session session, ImapProperties imapProperties) {
        this.session = session;
        this.imapProperties = imapProperties;
    }

    public <T> T withInbox(FolderOperation<T> folderOperation) throws MessagingException {
        Store store = null;
        Folder folder = null;

        try {
            store = session.getStore("imaps");
            store.connect(
                    imapProperties.host(),
                    imapProperties.port(),
                    imapProperties.username(),
                    imapProperties.password()
            );

            folder = store.getFolder(imapProperties.folder());
            folder.open(Folder.READ_WRITE);

            return folderOperation.execute(folder);
        } finally {
            if (folder != null && folder.isOpen()) {
                folder.close(false);
            }
            if (store != null && store.isConnected()) {
                store.close();
            }
        }
    }
}
