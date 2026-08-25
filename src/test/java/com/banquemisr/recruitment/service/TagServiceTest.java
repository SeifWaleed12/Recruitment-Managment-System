package com.banquemisr.recruitment.service;

import com.banquemisr.recruitment.data.entity.TagEntity;
import com.banquemisr.recruitment.data.repo.TagRepo;
import com.banquemisr.recruitment.exception.DuplicateResourceException;
import com.banquemisr.recruitment.exception.ResourceNotFoundException;
import com.banquemisr.recruitment.mapper.TagMapper;
import com.banquemisr.recruitment.web.DTOs.request.TagRequest;
import com.banquemisr.recruitment.web.DTOs.respond.TagRespond;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepo tagRepo;

    @Mock
    private TagMapper tagMapper;

    @InjectMocks
    private TagService tagService;

    @Test
    void getAllTags_returnsMappedList() {

        TagEntity entity = TagEntity.builder().tagId("tag-1").name("Backend").build();
        TagRespond respond = TagRespond.builder().id("tag-1").name("Backend").build();

        when(tagRepo.findAll()).thenReturn(List.of(entity));
        when(tagMapper.toRespond(entity)).thenReturn(respond);

        List<TagRespond> result = tagService.getAllTags();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Backend");
    }

    @Test
    void createTag_whenNameUnique_savesAndReturnsRespond() {

        TagRequest request = TagRequest.builder().name("Frontend").build();
        TagEntity toSave = TagEntity.builder().name("Frontend").build();
        TagEntity saved = TagEntity.builder().tagId("tag-2").name("Frontend").build();
        TagRespond respond = TagRespond.builder().id("tag-2").name("Frontend").build();

        when(tagRepo.findByNameIgnoreCase("Frontend")).thenReturn(Optional.empty());
        when(tagMapper.toEntity(request)).thenReturn(toSave);
        when(tagRepo.save(toSave)).thenReturn(saved);
        when(tagMapper.toRespond(saved)).thenReturn(respond);

        TagRespond result = tagService.createTag(request);

        assertThat(result.getId()).isEqualTo("tag-2");
        verify(tagRepo).save(toSave);
    }

    @Test
    void createTag_whenNameAlreadyExists_throwsDuplicateResourceException() {

        TagRequest request = TagRequest.builder().name("Backend").build();
        TagEntity existing = TagEntity.builder().tagId("tag-1").name("Backend").build();

        when(tagRepo.findByNameIgnoreCase("Backend")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> tagService.createTag(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(tagRepo, never()).save(any());
    }

    @Test
    void deleteTag_whenExists_deletesTag() {

        when(tagRepo.existsById("tag-1")).thenReturn(true);

        tagService.deleteTag("tag-1");

        verify(tagRepo).deleteById("tag-1");
    }

    @Test
    void deleteTag_whenNotFound_throwsResourceNotFoundException() {

        when(tagRepo.existsById("missing")).thenReturn(false);

        assertThatThrownBy(() -> tagService.deleteTag("missing"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(tagRepo, never()).deleteById(any());
    }

    @Test
    void getTagEntityById_whenExists_returnsEntity() {

        TagEntity entity = TagEntity.builder().tagId("tag-1").name("Backend").build();

        when(tagRepo.findById("tag-1")).thenReturn(Optional.of(entity));

        TagEntity result = tagService.getTagEntityById("tag-1");

        assertThat(result.getName()).isEqualTo("Backend");
    }

    @Test
    void getTagEntityById_whenNotFound_throwsResourceNotFoundException() {

        when(tagRepo.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.getTagEntityById("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}