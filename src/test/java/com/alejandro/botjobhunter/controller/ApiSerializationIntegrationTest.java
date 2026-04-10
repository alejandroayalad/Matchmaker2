package com.alejandro.botjobhunter.controller;

import com.alejandro.botjobhunter.models.Application;
import com.alejandro.botjobhunter.models.Company;
import com.alejandro.botjobhunter.models.Job;
import com.alejandro.botjobhunter.models.User;
import com.alejandro.botjobhunter.models.enums.ApplicationStatus;
import com.alejandro.botjobhunter.models.enums.JobSource;
import com.alejandro.botjobhunter.repository.ApplicationRepository;
import com.alejandro.botjobhunter.repository.CompanyRepository;
import com.alejandro.botjobhunter.repository.JobRepository;
import com.alejandro.botjobhunter.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiSerializationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        applicationRepository.deleteAll();
        jobRepository.deleteAll();
        companyRepository.deleteAll();
    }

    @Test
    void jobsEndpointShouldSerializeCompanyWithoutLazyLoadingErrors() throws Exception {
        Company company = companyRepository.save(Company.builder()
                .name("Acme")
                .website("https://acme.test")
                .build());

        Job job = jobRepository.save(Job.builder()
                .title("Backend Engineer")
                .company(company)
                .source(JobSource.MANUAL)
                .active(true)
                .scrappedAt(LocalDateTime.now())
                .build());

        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(job.getId()))
                .andExpect(jsonPath("$[0].company.name").value("Acme"));

        mockMvc.perform(get("/api/jobs/{id}", job.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.company.name").value("Acme"));
    }

    @Test
    void applicationsEndpointShouldSerializeNestedUserAndJobWithoutLazyLoadingErrors() throws Exception {
        User user = userRepository.findAll().stream().findFirst().orElseGet(() ->
                userRepository.save(User.builder()
                        .firstName("Test")
                        .lastName("User")
                        .email("test@example.com")
                        .build())
        );

        Company company = companyRepository.save(Company.builder()
                .name("Globex")
                .build());

        Job job = jobRepository.save(Job.builder()
                .title("Full Stack Developer")
                .company(company)
                .source(JobSource.MANUAL)
                .active(true)
                .scrappedAt(LocalDateTime.now())
                .build());

        Application application = applicationRepository.save(Application.builder()
                .user(user)
                .job(job)
                .status(ApplicationStatus.SAVED)
                .appliedAt(LocalDateTime.now())
                .build());

        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(application.getId()))
                .andExpect(jsonPath("$[0].user.id").value(user.getId()))
                .andExpect(jsonPath("$[0].job.title").value("Full Stack Developer"))
                .andExpect(jsonPath("$[0].job.company.name").value("Globex"));
    }
}
