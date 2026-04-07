package com.alejandro.botjobhunter.service.email;

import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class MimeBodyExtractor {

    public String extractHtmlBody(Message message) throws MessagingException, IOException {
        return extractBestBody(message, true);
    }

    private String extractBestBody(Part part, boolean preferHtml) throws MessagingException, IOException {
        if (part.isMimeType("text/html")) {
            return readContentAsString(part);
        }

        if (!preferHtml && part.isMimeType("text/plain")) {
            return readContentAsString(part);
        }

        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            String plainTextFallback = null;

            for (int index = 0; index < multipart.getCount(); index++) {
                BodyPart bodyPart = multipart.getBodyPart(index);
                String extracted = extractBestBody(bodyPart, preferHtml);
                if (hasText(extracted)) {
                    if (bodyPart.isMimeType("text/html")) {
                        return extracted;
                    }
                    if (plainTextFallback == null) {
                        plainTextFallback = extracted;
                    }
                }
            }

            return plainTextFallback;
        }

        if (part.isMimeType("message/rfc822")) {
            Object nested = part.getContent();
            if (nested instanceof Part nestedPart) {
                return extractBestBody(nestedPart, preferHtml);
            }
        }

        if (preferHtml) {
            return extractBestBody(part, false);
        }

        return null;
    }

    private String readContentAsString(Part part) throws MessagingException, IOException {
        Object content = part.getContent();
        return content instanceof String body && hasText(body) ? body : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
