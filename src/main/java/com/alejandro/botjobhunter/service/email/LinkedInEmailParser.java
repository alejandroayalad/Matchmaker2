package com.alejandro.botjobhunter.service.email;

import com.alejandro.botjobhunter.dto.EmailJobResultDTO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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

    public List<EmailJobResultDTO> parse(String htmlBody) {
        if (htmlBody == null || htmlBody.isBlank()) {
            return List.of();
        }

        Document document = Jsoup.parse(htmlBody);
        Elements jobLinks = document.select(
                "a[href*=/jobs/view][style*=font-weight:600], " +
                "a[href*=linkedin.com/comm/jobs/view][style*=font-weight:600]"
        );
        Map<String, EmailJobResultDTO> jobsByUrl = new LinkedHashMap<>();

        for (Element link : jobLinks) {
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

    private String normalizeJobUrl(String href) {
        String decoded = decodeTrackingUrl(href);
        if (decoded == null || !decoded.toLowerCase(Locale.ROOT).contains("linkedin.com")) {
            return null;
        }

        Matcher matcher = JOB_ID_PATTERN.matcher(decoded);
        if (!matcher.find()) {
            return null;
        }

        return "https://www.linkedin.com/jobs/view/" + matcher.group(1);
    }

    private String decodeTrackingUrl(String href) {
        if (href == null || href.isBlank()) {
            return null;
        }

        try {
            URI uri = URI.create(href);
            String query = uri.getRawQuery();
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] parts = param.split("=", 2);
                    if (parts.length == 2 && List.of("url", "redirect", "dest", "destination").contains(parts[0].toLowerCase(Locale.ROOT))) {
                        return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                    }
                }
            }
            return href;
        } catch (IllegalArgumentException ignored) {
            return href;
        }
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
