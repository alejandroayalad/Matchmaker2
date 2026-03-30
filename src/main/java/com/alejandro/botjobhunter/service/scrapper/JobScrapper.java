package com.alejandro.botjobhunter.service.scrapper;
import com.alejandro.botjobhunter.models.Job;

import java.util.List;


public interface JobScrapper {

    List<Job> searchJobs(String query, String location);

}
