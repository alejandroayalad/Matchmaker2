package com.alejandro.botjobhunter.service.importer;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PageFetcher {
    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36";
    private static final int REQUEST_TIMEOUT_MILLIS = 15_000;

    public Document fetchDocument(String url) {
        try {
            return Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .referrer("https://www.google.com/")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Cache-Control", "no-cache")
                    .header("Pragma", "no-cache")
                    .maxBodySize(0)
                    .timeout(REQUEST_TIMEOUT_MILLIS)
                    .get();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to fetch page: " + url, exception);
        }
    }
}
