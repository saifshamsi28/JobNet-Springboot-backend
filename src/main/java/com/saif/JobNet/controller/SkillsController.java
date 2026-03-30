package com.saif.JobNet.controller;

import com.saif.JobNet.model.JobNetResponse;
import com.saif.JobNet.services.SkillsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/skills")
public class SkillsController {

    private final SkillsService skillsService;

    public SkillsController(SkillsService skillsService) {
        this.skillsService = skillsService;
    }

    @GetMapping
    public ResponseEntity<?> getSkillsByType(
            @RequestParam("type") String type,
            @RequestParam(value = "limit", defaultValue = "500") int limit
    ) {
        try {
            List<String> skills = skillsService.getSkillsByType(type, limit);
            return ResponseEntity.ok(Map.of("data", skills, "source", "provider-or-fallback"));
        } catch (Exception ex) {
            List<String> fallback = skillsService.getFallbackSkillsByType(type, limit);
            return ResponseEntity.ok(Map.of("data", fallback, "source", "fallback", "warning", "skills provider unavailable"));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchSkills(
            @RequestParam("q") String query,
            @RequestParam(value = "limit", defaultValue = "10") int limit
    ) {
        try {
            List<String> skills = skillsService.searchSkills(query, limit);
            return ResponseEntity.ok(Map.of("data", skills, "source", "provider-or-fallback"));
        } catch (Exception ex) {
            List<String> fallback = skillsService.searchFallbackSkills(query, limit);
            return ResponseEntity.ok(Map.of("data", fallback, "source", "fallback", "warning", "skills provider unavailable"));
        }
    }
}
