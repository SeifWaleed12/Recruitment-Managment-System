package com.banquemisr.recruitment.mapper;

import com.banquemisr.recruitment.data.entity.TagEntity;
import com.banquemisr.recruitment.web.DTOs.request.TagRequest;
import com.banquemisr.recruitment.web.DTOs.respond.TagRespond;
import org.springframework.stereotype.Component;


@Component
public class TagMapper {
    public TagEntity toEntity(TagRequest request){
        if (request==null)return null;
        return TagEntity.builder().name(request.getName()).build();
    }

    public TagRespond toRespond(TagEntity entity){
        if (entity== null) return null;
        return TagRespond.builder().id(entity.getTagId()).name(entity.getName()).build();
    }
}
