package com.saif.JobNet.model.resume;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ParsedResumeDetails {
        private String name;
        private String email;
        private String phone;
        private String linkedin;
        private String github;
        private String portfolio;
        private List<String> skills;
        private String experience;
        @JsonProperty("raw_text_snippet")
        private String rawTextSnippet;
}
