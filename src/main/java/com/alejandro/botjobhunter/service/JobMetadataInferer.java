package com.alejandro.botjobhunter.service;

import com.alejandro.botjobhunter.models.enums.ExperienceLevel;
import com.alejandro.botjobhunter.models.enums.JobType;

import java.text.Normalizer;
import java.util.Locale;
import java.util.StringJoiner;

public final class JobMetadataInferer {
    private JobMetadataInferer() {
    }

    public static ExperienceLevel inferExperienceLevel(String... values) {
        String text = normalize(values);
        if (text.isBlank()) {
            return ExperienceLevel.MID;
        }

        if (containsAny(text, "intern", "entry level", "entry-level", "trainee")) {
            return ExperienceLevel.ENTRY;
        }
        if (containsAny(text, "junior", "associate")) {
            return ExperienceLevel.JUNIOR;
        }
        if (containsAny(text, "senior", "staff", "principal", "lead")) {
            return ExperienceLevel.SENIOR;
        }

        return ExperienceLevel.MID;
    }

    public static JobType inferJobType(String... values) {
        String text = normalize(values);
        if (text.isBlank()) {
            return null;
        }

        if (containsAny(text, "hybrid", "hibrido", "hibrida")) {
            return JobType.HYBRID;
        }
        if (containsAny(text, "remote", "remoto", "remota", "work from home", "wfh", "distributed", "anywhere")) {
            return JobType.REMOTE;
        }
        if (containsAny(text, "onsite", "on-site", "in office", "in-office", "presencial")) {
            return JobType.ONSITE;
        }

        return null;
    }

    private static boolean containsAny(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(token)) {
                return true;
            }
        }

        return false;
    }

    private static String normalize(String... values) {
        StringJoiner joiner = new StringJoiner(" ");
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }

            joiner.add(normalizeSingle(value));
        }

        return joiner.toString().trim();
    }

    private static String normalizeSingle(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace('\u00A0', ' ')
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
