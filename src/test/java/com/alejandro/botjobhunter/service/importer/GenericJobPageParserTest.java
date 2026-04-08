package com.alejandro.botjobhunter.service.importer;

import com.alejandro.botjobhunter.dto.UrlImportResultDTO;
import com.alejandro.botjobhunter.models.enums.JobSource;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenericJobPageParserTest {

    @Test
    void canHandleShouldAcceptHttpAndHttpsUrls() {
        GenericJobPageParser parser = new GenericJobPageParser(new PageFetcher());

        assertTrue(parser.canHandle("https://example.com/jobs/123"));
        assertTrue(parser.canHandle("http://example.com/jobs/123"));
        assertFalse(parser.canHandle("mailto:test@example.com"));
        assertFalse(parser.canHandle(null));
        assertEquals(JobSource.MANUAL, parser.getJobSource());
    }

    @Test
    void importFromUrlShouldExtractFromMetaTagsAndCommonSelectors() {
        String html = """
                <html>
                  <head>
                    <title>Senior Backend Engineer - Acme Careers</title>
                    <meta property="og:title" content="Senior Backend Engineer - Acme Careers" />
                    <meta property="og:site_name" content="Acme Careers" />
                    <meta name="description" content="Build resilient backend services for job ingestion." />
                  </head>
                  <body>
                    <main>
                      <div class="company-name">Acme</div>
                      <div class="job-location">Remote, Mexico</div>
                      <div class="salary-range">MXN 90,000 - 110,000 monthly</div>
                    </main>
                  </body>
                </html>
                """;

        PageFetcher pageFetcher = new PageFetcher() {
            @Override
            public Document fetchDocument(String url) {
                return Jsoup.parse(html, url);
            }
        };
        GenericJobPageParser parser = new GenericJobPageParser(pageFetcher);

        UrlImportResultDTO result = parser.importFromUrl("https://careers.example.com/jobs/123");

        assertEquals("Senior Backend Engineer", result.title());
        assertEquals("Acme", result.companyName());
        assertEquals("Remote, Mexico", result.location());
        assertEquals("Build resilient backend services for job ingestion.", result.description());
        assertEquals("MXN 90,000 - 110,000 monthly", result.salary());
        assertEquals("https://careers.example.com/jobs/123", result.url());
    }

    @Test
    void importFromUrlShouldRejectUnsupportedUrls() {
        GenericJobPageParser parser = new GenericJobPageParser(new PageFetcher());

        assertThrows(
                IllegalArgumentException.class,
                () -> parser.importFromUrl("mailto:test@example.com")
        );
    }
}
