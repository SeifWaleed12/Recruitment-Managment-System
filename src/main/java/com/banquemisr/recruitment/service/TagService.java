package com.banquemisr.recruitment.service;

import com.banquemisr.recruitment.data.entity.TagEntity;
import com.banquemisr.recruitment.data.repo.TagRepo;
import com.banquemisr.recruitment.exception.DuplicateResourceException;
import com.banquemisr.recruitment.exception.ResourceNotFoundException;
import com.banquemisr.recruitment.mapper.TagMapper;
import com.banquemisr.recruitment.web.DTOs.request.TagRequest;
import com.banquemisr.recruitment.web.DTOs.respond.TagRespond;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@RequiredArgsConstructor

public class TagService {

    private final TagRepo tagRepo;
    private final TagMapper tagMapper;

    public List<TagRespond> getAllTags(){
        return tagRepo.findAll().stream().map(tagMapper::toRespond).toList();
    }

    public TagRespond createTag(TagRequest request){
        tagRepo.findByNameIgnoreCase(request.getName()).ifPresent(t ->
        {throw new DuplicateResourceException("Tag already exists " + request.getName());
        });

        TagEntity saved = tagRepo.save(tagMapper.toEntity(request));
        return tagMapper.toRespond(saved);
    }

    public void deleteTag(String id){
        if (!tagRepo.existsById(id)) {
            throw new ResourceNotFoundException("Tag not found with ID: " + id);
        }
        tagRepo.deleteById(id);
    }

    private TagRespond toRespond(TagEntity entity){
        return TagRespond.builder()
                .id(entity.getTagId())
                .name(entity.getName())
                .build();
    }

}
