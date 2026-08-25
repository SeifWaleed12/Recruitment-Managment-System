package com.banquemisr.recruitment.service;

import com.banquemisr.recruitment.data.entity.SkillEntity;
import com.banquemisr.recruitment.data.repo.SkillRepo;
import com.banquemisr.recruitment.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

    @Mock
    private SkillRepo skillRepo;

    @InjectMocks
    private SkillService skillService;

    @Test
    void findOrCreate_whenExists_returnsExistingSkillWithoutSaving() {

        SkillEntity existing = SkillEntity.builder().id("skill-1").name("Java").build();

        when(skillRepo.findByNameIgnoreCase("Java")).thenReturn(Optional.of(existing));

        SkillEntity result = skillService.findOrCreate("Java");

        assertThat(result).isEqualTo(existing);
        verify(skillRepo, never()).save(any());
    }

    @Test
    void findOrCreate_whenMissing_createsAndReturnsNewSkill() {

        SkillEntity saved = SkillEntity.builder().id("skill-2").name("Python").build();

        when(skillRepo.findByNameIgnoreCase("Python")).thenReturn(Optional.empty());
        when(skillRepo.save(any(SkillEntity.class))).thenReturn(saved);

        SkillEntity result = skillService.findOrCreate("Python");

        assertThat(result.getName()).isEqualTo("Python");
        verify(skillRepo).save(argThat(entity -> entity.getName().equals("Python")));
    }

    @Test
    void getSkillEntityById_whenExists_returnsEntity() {

        SkillEntity entity = SkillEntity.builder().id("skill-1").name("Java").build();

        when(skillRepo.findById("skill-1")).thenReturn(Optional.of(entity));

        SkillEntity result = skillService.getSkillEntityById("skill-1");

        assertThat(result.getName()).isEqualTo("Java");
    }

    @Test
    void getSkillEntityById_whenNotFound_throwsResourceNotFoundException() {

        when(skillRepo.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> skillService.getSkillEntityById("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}