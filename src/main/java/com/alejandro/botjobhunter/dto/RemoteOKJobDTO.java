
package com.alejandro.botjobhunter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteOKJobDTO {

    private String id;
    private String company;
    private String position;
    private String location;
    private String description;
    private String url;
    private String[] tags;

}