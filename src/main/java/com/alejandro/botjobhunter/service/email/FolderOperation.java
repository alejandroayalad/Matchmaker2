package com.alejandro.botjobhunter.service.email;

import jakarta.mail.Folder;
import jakarta.mail.MessagingException;

@FunctionalInterface
public interface FolderOperation<T> {
    T execute(Folder folder) throws MessagingException;
}
