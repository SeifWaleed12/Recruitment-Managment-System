package com.banquemisr.recruitment.service;

import com.banquemisr.recruitment.data.entity.TagEntity;
import com.banquemisr.recruitment.data.repo.TagRepo;

import java.util.List;

public class TagService {

    private final TagRepo tagRepo;

    public TagService(TagRepo tagRepo){
        this.tagRepo=tagRepo;
    }

    public TagEntity CreateTag(String name){
        tagRepo.findByNameIgnoreCase(name).ifPresent(t -> {
            throw new IllegalArgumentException("Tag already exists: "+ name);
        });
        return tagRepo.save(TagEntity.builder().name(name).build());
    }

    public List<TagEntity> getAllTags(){
        return tagRepo.findAll();
    }

    public void deleteTag(String id){
        tagRepo.deleteById(id);
    }

}
