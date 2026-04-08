package com.alejandro.botjobhunter.service.importer;

import com.alejandro.botjobhunter.dto.UrlImportResultDTO;
import com.alejandro.botjobhunter.models.enums.JobSource;
import org.jsoup.nodes.Document;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@Order(2)
public class GenericJobPageParser implements UrlJobImporter {
    private static final List<String> TITLE_SELECTORS = List.of(
            "h1",
            "[data-testid*='job-title']",
            "[class*='job-title']"
    );
    private static final List<String> COMPANY_SELECTORS = List.of(
            "[data-company]",
            "[data-company-name]",
            "[itemprop='hiringOrganization']",
            "[class*='company']"
    );
    private static final List<String> LOCATION_SELECTORS = List.of(
            "[data-location]",
            "[itemprop='jobLocation']",
            "[class*='location']"
    );
    private static final List<String> DESCRIPTION_SELECTORS = List.of(
            "[data-testid*='description']",
            "[class*='description']",
            "article",
            "main"
    );
    private static final List<String> SALARY_SELECTORS = List.of(
            "[data-salary]",
            "[itemprop='baseSalary']",
            "[class*='salary']",
            "[class*='compensation']"
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
    private static final List<String> META_COMPANY_SELECTORS = List.of(
            "meta[property='og:site_name']",
            "meta[name='application-name']"
    );
    private static final List<String> META_LOCATION_SELECTORS = List.of(
            "meta[property='job:location']",
            "meta[name='job:location']",
            "meta[property='place:location:address']"
    );

    private final PageFetcher pageFetcher;

    public GenericJobPageParser(PageFetcher pageFetcher) {
        this.pageFetcher = pageFetcher;
    }

    @Override
    public boolean canHandle(String url) {
        if (url == null) {
            return false;
        }

        String normalized = url.toLowerCase(Locale.ROOT);
        return normalized.startsWith("https://") || normalized.startsWith("http://");
    }

    @Override
    public JobSource getJobSource() {
        return JobSource.MANUAL;
    }

    @Override
    public UrlImportResultDTO importFromUrl(String url) {
        if (!canHandle(url)) {
            throw new IllegalArgumentException("Unsupported job URL: " + url);
        }

        Document document = pageFetcher.fetchDocument(url);
        return parseDocument(document, url);
    }

    UrlImportResultDTO parseDocument(Document document, String url) {
        String siteName = HtmlExtractionUtils.selectFirstMetaContent(document, META_COMPANY_SELECTORS);
        String title = HtmlExtractionUtils.firstNonBlank(
                normalizeTitle(HtmlExtractionUtils.selectFirstText(document, TITLE_SELECTORS), siteName),
                normalizeTitle(HtmlExtractionUtils.selectFirstMetaContent(document, META_TITLE_SELECTORS), siteName),
                normalizeTitle(HtmlExtractionUtils.clean(document.title()), siteName)
        );
        String companyName = HtmlExtractionUtils.firstNonBlank(
                HtmlExtractionUtils.selectFirstText(document, COMPANY_SELECTORS),
                siteName
        );
        String location = HtmlExtractionUtils.firstNonBlank(
                HtmlExtractionUtils.selectFirstText(document, LOCATION_SELECTORS),
                HtmlExtractionUtils.selectFirstMetaContent(document, META_LOCATION_SELECTORS)
        );
        String description = HtmlExtractionUtils.firstNonBlank(
                HtmlExtractionUtils.selectFirstMetaContent(document, META_DESCRIPTION_SELECTORS),
                HtmlExtractionUtils.selectFirstText(document, DESCRIPTION_SELECTORS)
        );
        String salary = HtmlExtractionUtils.selectFirstText(document, SALARY_SELECTORS);

        if (title == null && companyName == null && location == null && description == null && salary == null) {
            throw new IllegalStateException("Could not extract job details from page: " + url);
        }

        return new UrlImportResultDTO(title, companyName, location, description, salary, url);
    }

    private String normalizeTitle(String title, String siteName) {
        String cleanedTitle = HtmlExtractionUtils.clean(title);
        if (cleanedTitle == null) {
            return null;
        }

        String cleanedSiteName = HtmlExtractionUtils.clean(siteName);
        if (cleanedSiteName == null) {
            return cleanedTitle;
        }

        String pattern = "\\s*[|\\-]\\s*" + Pattern.quote(cleanedSiteName) + "\\s*$";
        return HtmlExtractionUtils.clean(cleanedTitle.replaceFirst(pattern, ""));
    }
}
