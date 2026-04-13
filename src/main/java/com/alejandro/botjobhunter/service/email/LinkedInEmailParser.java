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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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

            Element row = link.closest("tr");
            Element table = link.closest("table");
            Element cell = row != null ? row : table;
            String companyName = null;
            String location = null;
            String summary = null;
            List<String> insights = List.of();

            if (cell != null) {
                String companyAndLocation = extractCompanyLocation(cell);
                if (companyAndLocation == null && table != null && table != cell) {
                    companyAndLocation = extractCompanyLocation(table);
                }
                CompanyLocation details = splitCompanyLocation(companyAndLocation);
                companyName = details.companyName();
                location = details.location();

                List<String> supportingLines = extractSupportingLines(cell, title, companyAndLocation, companyName, location);
                summary = extractSummary(supportingLines);
                insights = extractInsights(supportingLines, summary);
            }

            jobsByUrl.putIfAbsent(url, new EmailJobResultDTO(title, companyName, location, url, summary, insights));
        }

        return new ArrayList<>(jobsByUrl.values());
    }

    private String extractCompanyLocation(Element card) {
        Element preferredParagraph = card.selectFirst("p[style*=color:#1f1f1f][style*=font-size:12px]");
        if (preferredParagraph != null) {
            return clean(preferredParagraph.text());
        }

        for (Element element : card.select("p, span, div")) {
            String text = clean(element.text());
            if (text != null && text.contains(" · ")) {
                return text;
            }
        }

        return null;
    }

    private CompanyLocation splitCompanyLocation(String companyAndLocation) {
        if (companyAndLocation == null) {
            return new CompanyLocation(null, null);
        }

        String[] parts = companyAndLocation.split(" · ", 2);
        String companyName = parts.length > 0 ? clean(parts[0]) : null;
        String location = parts.length > 1 ? clean(parts[1]) : null;
        return new CompanyLocation(companyName, location);
    }

    private List<String> extractSupportingLines(
            Element card,
            String title,
            String companyLocation,
            String companyName,
            String location
    ) {
        Set<String> lines = new LinkedHashSet<>();

        for (Element element : card.select("p, li, span, div")) {
            String text = clean(element.text());
            if (text == null) {
                continue;
            }

            if (text.equals(title)
                    || text.equals(companyLocation)
                    || text.equals(companyName)
                    || text.equals(location)
                    || text.contains(title + " " + companyLocation)) {
                continue;
            }

            lines.add(text);
        }

        return new ArrayList<>(lines);
    }

    private String extractSummary(List<String> supportingLines) {
        for (String line : supportingLines) {
            if (line.length() >= 60 || wordCount(line) >= 10) {
                return line;
            }
        }

        return null;
    }

    private List<String> extractInsights(List<String> supportingLines, String summary) {
        List<String> insights = new ArrayList<>();

        for (String line : supportingLines) {
            if (line.equals(summary)) {
                continue;
            }

            for (String part : splitInsightCandidates(line)) {
                if (part.length() < 3 || part.length() > 90 || containsIgnoreCase(part, "linkedin")) {
                    continue;
                }

                insights.add(part);
                if (insights.size() == 4) {
                    return insights;
                }
            }
        }

        return insights;
    }

    private List<String> splitInsightCandidates(String line) {
        List<String> parts = new ArrayList<>();
        for (String candidate : line.split("\\s*[·•|]\\s*")) {
            String cleaned = clean(candidate);
            if (cleaned != null) {
                parts.add(cleaned);
            }
        }
        return parts;
    }

    private int wordCount(String value) {
        return value == null || value.isBlank() ? 0 : value.trim().split("\\s+").length;
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

    private record CompanyLocation(String companyName, String location) {
    }
}
