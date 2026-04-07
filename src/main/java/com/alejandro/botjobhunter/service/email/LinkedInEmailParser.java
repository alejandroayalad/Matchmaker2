package com.alejandro.botjobhunter.service.email;

import com.alejandro.botjobhunter.dto.EmailJobResultDTO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LinkedInEmailParser {

    private static final Pattern JOB_ID_PATTERN = Pattern.compile("/jobs/view/(\\d+)");
    private static final List<String> TRACKING_PARAM_NAMES = List.of("url", "redirect", "dest", "destination");
    public boolean isLikelyJobAlertEmail(String subject, String sender, String htmlBody) {
        if (htmlBody == null || htmlBody.isBlank()) {
            return false;
        }

        Document document = Jsoup.parse(htmlBody);
        boolean hasLinkedInSender = containsIgnoreCase(sender, "linkedin");
        boolean hasLinkedInJobSubject = containsIgnoreCase(subject, "linkedin")
                && containsAnyIgnoreCase(subject, "job", "jobs", "alert", "empleo", "vacante");
        boolean hasLinkedInBranding = containsIgnoreCase(document.text(), "linkedin")
                || !document.select("a[href*=linkedin.com], img[alt*=LinkedIn]").isEmpty();

        return hasLinkedInBranding
                && !extractJobLinks(document).isEmpty()
                && (hasLinkedInSender || hasLinkedInJobSubject);
    }

    public List<EmailJobResultDTO> parse(String htmlBody) {
        if (htmlBody == null || htmlBody.isBlank()) {
            return List.of();
        }

        Document document = Jsoup.parse(htmlBody);
        Map<String, EmailJobResultDTO> jobsByUrl = new LinkedHashMap<>();

        for (Element link : extractJobLinks(document)) {
            String title = clean(link.text());
            String url = normalizeJobUrl(link.attr("href"));
            if (title == null || url == null) {
                continue;
            }

            Element cell = link.closest("table");
            String companyName = null;
            String location = null;

            if (cell != null) {
                Element companyLocationParagraph =
                        cell.selectFirst("p[style*=color:#1f1f1f][style*=font-size:12px]");

                if (companyLocationParagraph != null) {
                    String companyAndLocation = clean(companyLocationParagraph.text());
                    if (companyAndLocation != null) {
                        String[] parts = companyAndLocation.split(" · ", 2);
                        companyName = parts.length > 0 ? clean(parts[0]) : null;
                        location = parts.length > 1 ? clean(parts[1]) : null;
                    }
                }
            }

            jobsByUrl.putIfAbsent(url, new EmailJobResultDTO(title, companyName, location, url));
        }

        return new ArrayList<>(jobsByUrl.values());
    }

    private List<Element> extractJobLinks(Document document) {
        return document.select("a[href]").stream()
                .filter(this::isCandidateJobLink)
                .toList();
    }

    private boolean isCandidateJobLink(Element link) {
        return link.getElementsByTag("a").size() == 1
                && clean(link.text()) != null
                && normalizeJobUrl(link.attr("href")) != null;
    }

    private String normalizeJobUrl(String href) {
        String decoded = decodeTrackingUrl(href);
        if (decoded == null) {
            return null;
        }

        Matcher matcher = JOB_ID_PATTERN.matcher(decoded);
        if (!matcher.find()) {
            return null;
        }

        if (!isLinkedInUrlCandidate(decoded)) {
            return null;
        }

        return "https://www.linkedin.com/jobs/view/" + matcher.group(1);
    }

    private String decodeTrackingUrl(String href) {
        if (href == null || href.isBlank()) {
            return null;
        }

        String current = href;

        for (int depth = 0; depth < 3; depth++) {
            try {
                URI uri = URI.create(current);
                String query = uri.getRawQuery();
                if (query == null) {
                    return current;
                }

                boolean foundRedirect = false;
                for (String param : query.split("&")) {
                    String[] parts = param.split("=", 2);
                    if (parts.length == 2 && TRACKING_PARAM_NAMES.contains(parts[0].toLowerCase(Locale.ROOT))) {
                        current = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                        foundRedirect = true;
                        break;
                    }
                }

                if (!foundRedirect) {
                    return current;
                }
            } catch (IllegalArgumentException ignored) {
                return current;
            }
        }

        return current;
    }

    private boolean isLinkedInUrlCandidate(String href) {
        try {
            URI uri = URI.create(href);
            String host = uri.getHost();
            if (host == null) {
                return href.startsWith("/");
            }
            return host.toLowerCase(Locale.ROOT).endsWith("linkedin.com");
        } catch (IllegalArgumentException ignored) {
            return href.startsWith("/") || href.toLowerCase(Locale.ROOT).contains("linkedin.com");
        }
    }

    private boolean containsIgnoreCase(String value, String token) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(token.toLowerCase(Locale.ROOT));
    }

    private boolean containsAnyIgnoreCase(String value, String... tokens) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String normalized = value.toLowerCase(Locale.ROOT);
        for (String token : tokens) {
            if (normalized.contains(token.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();

        return cleaned.isBlank() ? null : cleaned;
    }
}
