package com.alejandro.botjobhunter.service.scrapper;

import com.alejandro.botjobhunter.dto.IndeedResponseDTO;
import com.alejandro.botjobhunter.models.Company;
import com.alejandro.botjobhunter.models.Job;
import com.alejandro.botjobhunter.models.enums.JobSource;
import com.alejandro.botjobhunter.repository.CompanyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class IndeedScraper implements JobScrapper{

    private final HttpClient htppClient;
    private final ObjectMapper  objectMapper;
    private final CompanyRepository companyRepository;

    public IndeedScraper (ObjectMapper objectMapper, CompanyRepository companyRepository){
        this.htppClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
        this.companyRepository = companyRepository;
    }

    @Override
    public List<Job> searchJobs(String query, String location) {
        List<Job> parsedJobs = new ArrayList<>();

        String q = query != null ? query.replace(" ", "+") : "";
        String l = location != null ? location.replace(" ", "+") : "";
        String url = "https://mx.indeed.com/jobs?q=" + q + "&l=" + l;

        try {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "es-MX,es;q=0.8,en-US;q=0.5,en;q=0.3")
                    .GET()
                    .build();

            HttpResponse<String> response = htppClient.send(request, HttpResponse.BodyHandlers.ofString());
            String html = response.body();

            Pattern pattern = Pattern.compile("window\\.mosaic\\.providerData\\[\"mosaic-provider-jobcards\"\\]=(\\{.*?\\});\\s*window\\.mosaic\\.providerData", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {

                String jsonString = matcher.group(1);

                IndeedResponseDTO responseDto = objectMapper.readValue(jsonString, IndeedResponseDTO.class);


                if (responseDto.getMetaData() != null && responseDto.getMetaData().getMosaicProviderJobCardsModel() != null) {

                    List<IndeedResponseDTO.IndeedJobDTO> jobs = responseDto.getMetaData().getMosaicProviderJobCardsModel().getResults();

                    for (IndeedResponseDTO.IndeedJobDTO dto : jobs) {

                        if (dto.getBestTitle() == null || dto.getJobkey() == null) continue;

                        String companyName = dto.getCompany() != null ? dto.getCompany() : "Unknown";
                        Company company = companyRepository.findByName(companyName)
                                .orElseGet(() -> companyRepository.save(
                                        Company.builder().name(companyName).build()
                                ));

                        String cleanSnippet = dto.getSnippet() != null ? dto.getSnippet().replaceAll("<[^>]*>", "").trim() : "";
                        String salary = dto.getSalarySnippet() != null ? dto.getSalarySnippet().getText() : "";

                        Job newJob = Job.builder()
                                .title(dto.getBestTitle())
                                .company(company)
                                .location(dto.getFormattedLocation())
                                .description(cleanSnippet)
                                .urlApplication("https://mx.indeed.com/viewjob?jk=" + dto.getJobkey())
                                .source(JobSource.INDEED)
                                .salary(salary)
                                .active(true)
                                .build();

                        parsedJobs.add(newJob);
                    }
                }
            } else {
                System.out.println("IndeedScraper: JSON payload not found. Indeed might be blocking or the structure changed.");
            }

            Thread.sleep(2000);

        } catch(Exception e) {
            System.out.println("Failed to fetch jobs from Indeed: " + e.getMessage());
        }

        return parsedJobs;

    }
}
