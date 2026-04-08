package com.alejandro.botjobhunter.service.importer;

import com.alejandro.botjobhunter.dto.UrlImportResultDTO;
import com.alejandro.botjobhunter.models.enums.JobSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkedInJobPageParserTest {

    @Test
    void canHandleShouldMatchLinkedInJobViewUrls() {
        LinkedInJobPageParser parser = new LinkedInJobPageParser(new ObjectMapper(), new PageFetcher());

        assertTrue(parser.canHandle("https://www.linkedin.com/jobs/view/1234567890/"));
        assertTrue(parser.canHandle("HTTPS://WWW.LINKEDIN.COM/JOBS/VIEW/1234567890/"));
        assertFalse(parser.canHandle("https://www.linkedin.com/company/openai"));
        assertFalse(parser.canHandle("https://example.com/jobs/view/1234567890/"));
        assertEquals(JobSource.LINKEDIN_URL, parser.getJobSource());
    }

    @Test
    void importFromUrlShouldExtractJobDetailsFromLinkedInHtml() {
        String html = """
                <html>
                  <head>
                    <title>Senior Backend Engineer | LinkedIn</title>
                  </head>
                  <body>
                    <main>
                      <section>
                        <h1 class="top-card-layout__title">Senior Backend Engineer</h1>
                        <a class="topcard__org-name-link" href="https://www.linkedin.com/company/acme">Acme Corp</a>
                        <span class="topcard__flavor topcard__flavor--bullet">Remote, Mexico</span>
                        <span class="compensation__salary">MXN 90,000 - 110,000 monthly</span>
                        <div class="show-more-less-html__markup">
                          Build APIs for job ingestion and integrations.
                        </div>
                      </section>
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
        LinkedInJobPageParser parser = new LinkedInJobPageParser(new ObjectMapper(), pageFetcher);

        UrlImportResultDTO result = parser.importFromUrl("https://www.linkedin.com/jobs/view/1234567890/");

        assertEquals("Senior Backend Engineer", result.title());
        assertEquals("Acme Corp", result.companyName());
        assertEquals("Remote, Mexico", result.location());
        assertEquals("Build APIs for job ingestion and integrations.", result.description());
        assertEquals("MXN 90,000 - 110,000 monthly", result.salary());
        assertEquals("https://www.linkedin.com/jobs/view/1234567890/", result.url());
    }

    @Test
    void importFromUrlShouldFallBackToMetaTagsAndStructuredData() {
        String html = """
                <html>
                  <head>
                    <meta property="og:title" content="Staff Platform Engineer | LinkedIn" />
                    <meta name="description" content="Design platform systems for scalable automation." />
                    <script type="application/ld+json">
                      {
                        "@context": "https://schema.org",
                        "@type": "JobPosting",
                        "title": "Staff Platform Engineer",
                        "description": "<p>Design platform systems for scalable automation.</p>",
                        "hiringOrganization": {
                          "@type": "Organization",
                          "name": "OpenAI"
                        },
                        "jobLocation": {
                          "@type": "Place",
                          "address": {
                            "@type": "PostalAddress",
                            "addressLocality": "San Francisco",
                            "addressRegion": "CA",
                            "addressCountry": "US"
                          }
                        },
                        "baseSalary": {
                          "@type": "MonetaryAmount",
                          "currency": "USD",
                          "value": {
                            "@type": "QuantitativeValue",
                            "minValue": 180000,
                            "maxValue": 220000,
                            "unitText": "YEAR"
                          }
                        }
                      }
                    </script>
                  </head>
                  <body></body>
                </html>
                """;

        PageFetcher pageFetcher = new PageFetcher() {
            @Override
            public Document fetchDocument(String url) {
                return Jsoup.parse(html, url);
            }
        };
        LinkedInJobPageParser parser = new LinkedInJobPageParser(new ObjectMapper(), pageFetcher);

        UrlImportResultDTO result = parser.importFromUrl("https://www.linkedin.com/jobs/view/987654321/");

        assertEquals("Staff Platform Engineer", result.title());
        assertEquals("OpenAI", result.companyName());
        assertEquals("San Francisco, CA, US", result.location());
        assertEquals("Design platform systems for scalable automation.", result.description());
        assertEquals("USD 180000 - 220000 YEAR", result.salary());
    }

    @Test
    void importFromUrlShouldRejectUnsupportedUrls() {
        LinkedInJobPageParser parser = new LinkedInJobPageParser(new ObjectMapper(), new PageFetcher());

        assertThrows(
                IllegalArgumentException.class,
                () -> parser.importFromUrl("https://example.com/jobs/view/1234567890/")
        );
    }
}
