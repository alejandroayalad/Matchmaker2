package com.alejandro.botjobhunter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IndeedResponseDTO {

    private IndeedMetaDataDTO metaData;
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IndeedMetaDataDTO {
        private IndeedJobCardsModelDTO mosaicProviderJobCardsModel;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IndeedJobCardsModelDTO {
        private List<IndeedJobDTO> results;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IndeedJobDTO {
        private String displayTitle;
        private String title;
        private String company;
        private String jobkey;
        private String formattedLocation;
        private String snippet;
        private IndeedSalarySnippetDTO salarySnippet;

        public String getBestTitle() {
            return (displayTitle != null && !displayTitle.isBlank()) ? displayTitle : title;
        }
    }
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IndeedSalarySnippetDTO {
        private String text;
    }
}