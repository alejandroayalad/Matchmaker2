package com.alejandro.botjobhunter.service.importer;

import com.alejandro.botjobhunter.dto.UrlImportResultDTO;
import com.alejandro.botjobhunter.models.enums.JobSource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Order(1)
public class LinkedInJobPageParser implements UrlJobImporter {
    private static final String LINKEDIN_JOB_URL_TOKEN = "linkedin.com/jobs/view/";
    private static final List<String> TITLE_SELECTORS = List.of(
            "h1.top-card-layout__title",
            "h1.topcard__title",
            "h1[data-test-id='job-title']",
            "h1[data-test-id='job-details-job-title']"
    );
    private static final List<String> COMPANY_SELECTORS = List.of(
            "a.topcard__org-name-link",
            "a[data-tracking-control-name='public_jobs_topcard-org-name']",
            ".topcard__flavor-row a[href*='/company/']"
    );
    private static final List<String> LOCATION_SELECTORS = List.of(
            "span.topcard__flavor--bullet",
            "span.topcard__flavor.topcard__flavor--bullet",
            "span.topcard__flavor--metadata",
            ".job-search-card__location"
    );
    private static final List<String> DESCRIPTION_SELECTORS = List.of(
            "div.show-more-less-html__markup",
            "div.description__text",
            "div.jobs-description__content",
            "section.show-more-less-html"
    );
    private static final List<String> SALARY_SELECTORS = List.of(
            ".compensation__salary",
            ".salary.compensation__salary",
            "[data-test-id='job-salary']",
            "[data-test-id*='salary']"
    );
    private static final List<String> META_TITLE_SELECTORS = List.of(
            "meta[property='og:title']",
            "meta[name='twitter:title']"
    );
    private static final List<String> META_DESCRIPTION_SELECTORS = List.of(
            "meta[name='description']",
            "meta[property='og:description']",
            "meta[name='twitter:description']"
    );

    private final ObjectMapper objectMapper;
    private final PageFetcher pageFetcher;

    public LinkedInJobPageParser(ObjectMapper objectMapper, PageFetcher pageFetcher) {
        this.objectMapper = objectMapper;
        this.pageFetcher = pageFetcher;
    }

    @Override
    public boolean canHandle(String url) {
        return url != null
                && url.toLowerCase(Locale.ROOT).contains(LINKEDIN_JOB_URL_TOKEN);
    }

    @Override
    public JobSource getJobSource() {
        return JobSource.LINKEDIN_URL;
    }

    @Override
    public UrlImportResultDTO importFromUrl(String url) {
        if (!canHandle(url)) {
            throw new IllegalArgumentException("Unsupported LinkedIn job URL: " + url);
        }

        Document document = pageFetcher.fetchDocument(url);
        return parseDocument(document, url);
    }

    UrlImportResultDTO parseDocument(Document document, String url) {
        JsonNode jobPosting = findJobPostingNode(document);

        String title = HtmlExtractionUtils.firstNonBlank(
                normalizeTitle(HtmlExtractionUtils.selectFirstText(document, TITLE_SELECTORS)),
                normalizeTitle(HtmlExtractionUtils.selectFirstMetaContent(document, META_TITLE_SELECTORS)),
                normalizeTitle(textAt(jobPosting, "title"))
        );
        String companyName = HtmlExtractionUtils.firstNonBlank(
                HtmlExtractionUtils.selectFirstText(document, COMPANY_SELECTORS),
                textAt(jobPosting, "hiringOrganization", "name")
        );
        String location = HtmlExtractionUtils.firstNonBlank(
                HtmlExtractionUtils.selectFirstText(document, LOCATION_SELECTORS),
                formatLocation(jobPosting)
        );
        String description = HtmlExtractionUtils.firstNonBlank(
                HtmlExtractionUtils.selectFirstText(document, DESCRIPTION_SELECTORS),
                HtmlExtractionUtils.selectFirstMetaContent(document, META_DESCRIPTION_SELECTORS),
                HtmlExtractionUtils.cleanHtml(textAt(jobPosting, "description"))
        );
        String salary = HtmlExtractionUtils.firstNonBlank(
                HtmlExtractionUtils.selectFirstText(document, SALARY_SELECTORS),
                formatBaseSalary(jobPosting)
        );

        if (title == null && companyName == null && location == null && description == null) {
            throw new IllegalStateException("Could not extract job details from LinkedIn page: " + url);
        }

        return new UrlImportResultDTO(title, companyName, location, description, salary, url);
    }

    private JsonNode findJobPostingNode(Document document) {
        for (Element script : document.select("script[type='application/ld+json']")) {
            String json = HtmlExtractionUtils.firstNonBlank(
                    HtmlExtractionUtils.clean(script.data()),
                    HtmlExtractionUtils.clean(script.html())
            );
            if (json == null) {
                continue;
            }

            try {
                JsonNode root = objectMapper.readTree(json);
                JsonNode jobPosting = findJobPostingNode(root);
                if (jobPosting != null) {
                    return jobPosting;
                }
            } catch (IOException ignored) {
            }
        }

        return null;
    }

    private JsonNode findJobPostingNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        if (node.isArray()) {
            for (JsonNode child : node) {
                JsonNode result = findJobPostingNode(child);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }

        if (!node.isObject()) {
            return null;
        }

        JsonNode typeNode = node.get("@type");
        if (hasType(typeNode, "JobPosting")) {
            return node;
        }

        for (JsonNode child : node) {
            JsonNode result = findJobPostingNode(child);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    private boolean hasType(JsonNode typeNode, String expectedType) {
        if (typeNode == null || typeNode.isNull()) {
            return false;
        }

        if (typeNode.isArray()) {
            for (JsonNode child : typeNode) {
                if (hasType(child, expectedType)) {
                    return true;
                }
            }
            return false;
        }

        return expectedType.equalsIgnoreCase(typeNode.asText(""));
    }

    private String formatLocation(JsonNode jobPosting) {
        String locality = textAt(jobPosting, "jobLocation", "address", "addressLocality");
        String region = textAt(jobPosting, "jobLocation", "address", "addressRegion");
        String country = textAt(jobPosting, "jobLocation", "address", "addressCountry");

        List<String> parts = new ArrayList<>();
        if (locality != null) {
            parts.add(locality);
        }
        if (region != null && !parts.contains(region)) {
            parts.add(region);
        }
        if (country != null && !parts.contains(country)) {
            parts.add(country);
        }

        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private String formatBaseSalary(JsonNode jobPosting) {
        JsonNode baseSalary = firstNode(jobPosting == null ? null : jobPosting.get("baseSalary"));
        if (baseSalary == null || baseSalary.isNull() || baseSalary.isMissingNode()) {
            return null;
        }

        if (baseSalary.isTextual() || baseSalary.isNumber()) {
            return HtmlExtractionUtils.clean(baseSalary.asText());
        }

        String currency = HtmlExtractionUtils.clean(baseSalary.path("currency").asText(null));
        JsonNode valueNode = firstNode(baseSalary.get("value"));

        if (valueNode != null && (valueNode.isTextual() || valueNode.isNumber())) {
            return joinWithSpaces(currency, HtmlExtractionUtils.clean(valueNode.asText()));
        }

        String minValue = HtmlExtractionUtils.clean(valueNode == null ? null : valueNode.path("minValue").asText(null));
        String maxValue = HtmlExtractionUtils.clean(valueNode == null ? null : valueNode.path("maxValue").asText(null));
        String unitText = HtmlExtractionUtils.clean(valueNode == null ? null : valueNode.path("unitText").asText(null));

        String range;
        if (minValue != null && maxValue != null) {
            range = minValue + " - " + maxValue;
        } else {
            range = HtmlExtractionUtils.firstNonBlank(minValue, maxValue);
        }

        return joinWithSpaces(currency, range, unitText);
    }

    private JsonNode firstNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        if (node.isArray()) {
            return node.isEmpty() ? null : node.get(0);
        }

        return node;
    }

    private String textAt(JsonNode root, String... path) {
        JsonNode current = root;
        for (String segment : path) {
            current = firstNode(current);
            if (current == null) {
                return null;
            }
            current = current.get(segment);
        }

        current = firstNode(current);
        if (current == null || current.isNull() || current.isMissingNode()) {
            return null;
        }

        return HtmlExtractionUtils.clean(current.asText());
    }

    private String normalizeTitle(String value) {
        String cleaned = HtmlExtractionUtils.clean(value);
        if (cleaned == null) {
            return null;
        }

        return HtmlExtractionUtils.clean(cleaned.replaceFirst("\\s*[|\\-]\\s*LinkedIn\\s*$", ""));
    }

    private String joinWithSpaces(String... values) {
        List<String> parts = new ArrayList<>();
        for (String value : values) {
            String cleaned = HtmlExtractionUtils.clean(value);
            if (cleaned != null) {
                parts.add(cleaned);
            }
        }

        return parts.isEmpty() ? null : String.join(" ", parts);
    }
}
