package com.alejandro.botjobhunter.service.email;

import com.alejandro.botjobhunter.dto.EmailJobResultDTO;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.FromStringTerm;
import jakarta.mail.search.OrTerm;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.SearchTerm;
import jakarta.mail.search.SubjectTerm;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Service
public class EmailOrchestrator {

    private final EmailConnectionService emailConnectionService;
    private final MimeBodyExtractor mimeBodyExtractor;
    private final LinkedInEmailParser linkedInEmailParser;

    public EmailOrchestrator(
            EmailConnectionService emailConnectionService,
            MimeBodyExtractor mimeBodyExtractor,
            LinkedInEmailParser linkedInEmailParser
    ) {
        this.emailConnectionService = emailConnectionService;
        this.mimeBodyExtractor = mimeBodyExtractor;
        this.linkedInEmailParser = linkedInEmailParser;
    }

    public List<EmailJobResultDTO> parseLinkedInEmails(Date since) throws MessagingException {
        return emailConnectionService.withInbox(folder -> {
            List<EmailJobResultDTO> results = new ArrayList<>();
            Message[] messages = folder.search(buildLinkedInSearchTerm(since));

            for (Message message : messages) {
                try {
                    String htmlBody = mimeBodyExtractor.extractHtmlBody(message);
                    results.addAll(linkedInEmailParser.parse(htmlBody));
                } catch (IOException exception) {
                    throw new MessagingException("Failed to extract MIME body for message: " + message.getSubject(), exception);
                }
            }

            return results;
        });
    }

    private SearchTerm buildLinkedInSearchTerm(Date since) {
        SearchTerm fromLinkedIn = new FromStringTerm("linkedin");
        SearchTerm subjectLinkedIn = new SubjectTerm("LinkedIn");
        SearchTerm linkedInTerm = new OrTerm(fromLinkedIn, subjectLinkedIn);

        if (since == null) {
            return linkedInTerm;
        }

        SearchTerm receivedSince = new ReceivedDateTerm(ComparisonTerm.GE, since);
        return new AndTerm(linkedInTerm, receivedSince);
    }
}
