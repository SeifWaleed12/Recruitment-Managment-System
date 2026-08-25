package com.banquemisr.recruitment.web.controller;

import com.banquemisr.recruitment.service.SkillService;
import com.banquemisr.recruitment.web.DTOs.request.SkillRequest;
import com.banquemisr.recruitment.web.DTOs.respond.SkillRespond;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'INTERVIEWER')")
    public List<SkillRespond> getAllSkills(@RequestParam(name = "query", required = false) String query) {
        return this.skillService.getAllSkills(query);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'INTERVIEWER')")
    public SkillRespond getSkillById(@PathVariable(name = "id") String skillId) {
        return this.skillService.getSkillById(skillId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public SkillRespond createSkill(@Valid @RequestBody SkillRequest request) {
        return this.skillService.createSkill(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public void deleteSkill(@PathVariable(name = "id") String skillId) {
        this.skillService.deleteSkill(skillId);
    }
}
