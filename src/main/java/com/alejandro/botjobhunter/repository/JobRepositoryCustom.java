package com.alejandro.botjobhunter.repository;

import com.alejandro.botjobhunter.models.Job;
import com.alejandro.botjobhunter.models.enums.ExperienceLevel;

import java.util.Collection;
import java.util.List;

public interface JobRepositoryCustom {
    List<Job> findMatchCandidates(
            Collection<ExperienceLevel> excludedSeniorities,
            String preferredLocation,
            Collection<String> blacklistedCompanies,
            Collection<String> negativeTitleKeywords,
            Collection<String> unsupportedLanguageTokens
    );
}
