package com.alejandro.botjobhunter.service.importer;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.List;

public final class HtmlExtractionUtils {
    private HtmlExtractionUtils() {
    }

    public static String selectFirstText(Document document, List<String> selectors) {
        for (String selector : selectors) {
            Element element = document.selectFirst(selector);
            if (element == null) {
                continue;
            }

            String text = clean(element.text());
            if (text != null) {
                return text;
            }
        }

        return null;
    }

    public static String selectFirstMetaContent(Document document, List<String> selectors) {
        for (String selector : selectors) {
            Element element = document.selectFirst(selector);
            if (element == null) {
                continue;
            }

            String text = clean(element.attr("content"));
            if (text != null) {
                return text;
            }
        }

        return null;
    }

    public static String firstNonBlank(String... values) {
        for (String value : values) {
            String cleaned = clean(value);
            if (cleaned != null) {
                return cleaned;
            }
        }

        return null;
    }

    public static String cleanHtml(String value) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return null;
        }

        return clean(Jsoup.parse(cleaned).text());
    }

    public static String clean(String value) {
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
