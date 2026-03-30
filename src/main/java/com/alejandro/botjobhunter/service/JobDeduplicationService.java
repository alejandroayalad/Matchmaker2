package com.alejandro.botjobhunter.service;

import com.alejandro.botjobhunter.models.Job;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class JobDeduplicationService {

    public List<Job> deduplicate(List<Job> jobs) {
        List<Job> uniqueJobs = new ArrayList<>();
        Set<String> seenHashes = new HashSet<>();

        for (Job job : jobs) {
            String hash = generateHash(job);

            if (seenHashes.add(hash)) {
                uniqueJobs.add(job);
            }
        }
        return uniqueJobs;
    }

    private String generateHash(Job job) {
        try {

            String title = job.getTitle() != null ? job.getTitle().toLowerCase().trim() : "";
            String company = (job.getCompany() != null && job.getCompany().getName() != null)
                    ? job.getCompany().getName().toLowerCase().trim() : "";


            String url = "";
            if (job.getUrlApplication() != null) {
                url = job.getUrlApplication().split("\\?")[0].toLowerCase().trim();
            }

            String rawString = title + "|" + company + "|" + url;

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(rawString.getBytes(StandardCharsets.UTF_8));


            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}