package com.alejandro.botjobhunter.service.scrapper;

import com.alejandro.botjobhunter.dto.RemoteOKJobDTO;
import com.alejandro.botjobhunter.models.Company;
import com.alejandro.botjobhunter.models.Job;
import com.alejandro.botjobhunter.models.enums.JobSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.jsoup.Jsoup;
import com.alejandro.botjobhunter.repository.CompanyRepository;

import java.util.Arrays;
import java.util.stream.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Service
public class RemoteOKScrapper implements JobScrapper {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final CompanyRepository companyRepository;



    public RemoteOKScrapper(ObjectMapper objectMapper, CompanyRepository companyRepository) {

        this.httpClient = HttpClient.newHttpClient();
        this.companyRepository = companyRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Job> searchJobs(String query, String location) {
        List<Job> parsedJobs = new ArrayList<>();

        String jsonResponseText = "";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://remoteok.com/api"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            jsonResponseText = response.body();

            RemoteOKJobDTO[] apiJobs = objectMapper.readValue(jsonResponseText, RemoteOKJobDTO[].class);

            for (int i = 1; i < apiJobs.length; i++) {
                RemoteOKJobDTO dto = apiJobs[i];



                if (location != null && !location.isBlank()) {
                    if (dto.getLocation() == null || !dto.getLocation().toLowerCase().contains(location.toLowerCase())) {
                        continue;
                    }
                }

                if (query != null && !query.isBlank()) {
                    String q = query.toLowerCase();


                    boolean matchesTitle = dto.getPosition() != null && dto.getPosition().toLowerCase().contains(q);


                    boolean matchesDesc = dto.getDescription() != null && dto.getDescription().toLowerCase().contains(q);


                    boolean matchesTags = false;
                    if (dto.getTags() != null) {
                        matchesTags = Arrays.stream(dto.getTags())
                                .anyMatch(tag -> tag.toLowerCase().contains(q));
                    }


                    if (!matchesTitle && !matchesDesc && !matchesTags) {
                        continue;
                    }
                }

                Company company = companyRepository.findByName(dto.getCompany())
                        .orElseGet(() -> companyRepository.save(
                                Company.builder().name(dto.getCompany()).build()
                        ));

                String cleanDescription;
                if (dto.getDescription() != null) cleanDescription = org.jsoup.Jsoup.parse(dto.getDescription()).text();
                else cleanDescription = "";


                Job newJob = Job.builder()
                        .title(dto.getPosition())
                        .company(company)
                        .location(dto.getLocation())
                        .description(cleanDescription)
                        .urlApplication(dto.getUrl())
                        .source(JobSource.REMOTEOK)
                        .active(true)
                        .build();

                parsedJobs.add(newJob);
            }

        } catch (Exception e) {
            System.out.println("Failed to fetch jobs from RemoteOK: " + e.getMessage());
        }

        return parsedJobs;
    }
}
