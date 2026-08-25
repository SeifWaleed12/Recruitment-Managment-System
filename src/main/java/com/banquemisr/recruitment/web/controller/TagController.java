package com.banquemisr.recruitment.web.controller;

import com.banquemisr.recruitment.service.TagService;
import com.banquemisr.recruitment.web.DTOs.request.TagRequest;
import com.banquemisr.recruitment.web.DTOs.respond.TagRespond;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {
    private final TagService tagService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'INTERVIEWER')")
    public List<TagRespond> getAllTags(){
        return this.tagService.getAllTags();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public TagRespond createTag(@Valid @RequestBody TagRequest tagRequest){
        return this.tagService.createTag(tagRequest);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public void deleteTag(@PathVariable(name = "id") String tagId){
        this.tagService.deleteTag(tagId);
    }
}
