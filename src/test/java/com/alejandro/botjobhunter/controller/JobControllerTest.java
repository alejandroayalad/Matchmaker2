package com.alejandro.botjobhunter.controller;

import com.alejandro.botjobhunter.dto.ScoreCard;
import com.alejandro.botjobhunter.models.Job;
import com.alejandro.botjobhunter.repository.JobRepository;
import com.alejandro.botjobhunter.service.matching.JobScoringService;
import com.alejandro.botjobhunter.service.scrapper.ScraperOrchestrator;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobControllerTest {

    @Test
    void getAllShouldUseSortedRepositoryQuery() {
        JobRepository jobRepository = mock(JobRepository.class);
        ScraperOrchestrator scraperOrchestrator = mock(ScraperOrchestrator.class);
        JobScoringService jobScoringService = mock(JobScoringService.class);
        JobController controller = new JobController(jobRepository, scraperOrchestrator, jobScoringService);

        List<Job> expected = List.of(Job.builder().title("Backend Engineer").build());
        when(jobRepository.findAllByOrderByActiveDescScrappedAtDesc()).thenReturn(expected);

        List<Job> result = controller.getAll();

        assertEquals(expected, result);
        verify(jobRepository).findAllByOrderByActiveDescScrappedAtDesc();
    }

    @Test
    void getMatchedJobsShouldDelegateToScoringService() {
        JobRepository jobRepository = mock(JobRepository.class);
        ScraperOrchestrator scraperOrchestrator = mock(ScraperOrchestrator.class);
        JobScoringService jobScoringService = mock(JobScoringService.class);
        JobController controller = new JobController(jobRepository, scraperOrchestrator, jobScoringService);

        List<ScoreCard> expected = List.of(
                new ScoreCard(10L, 42, List.of("Node.js"), true, true, List.of(), List.of("+42 Node.js"))
        );
        when(jobScoringService.getMatchedJobs(1L)).thenReturn(expected);

        List<ScoreCard> result = controller.getMatchedJobs(1L);

        assertEquals(expected, result);
        verify(jobScoringService).getMatchedJobs(1L);
    }

    @Test
    void getMatchedJobsShouldMapMissingUserToNotFound() {
        JobRepository jobRepository = mock(JobRepository.class);
        ScraperOrchestrator scraperOrchestrator = mock(ScraperOrchestrator.class);
        JobScoringService jobScoringService = mock(JobScoringService.class);
        JobController controller = new JobController(jobRepository, scraperOrchestrator, jobScoringService);

        when(jobScoringService.getMatchedJobs(99L)).thenThrow(new NoSuchElementException("User not found: 99"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.getMatchedJobs(99L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}
