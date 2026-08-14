package com.banquemisr.recruitment.data.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "candidates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 36)
    private String candidateId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "years_of_experience")
    @Builder.Default
    private Integer yearsOfExperience = 0;

    @Column(name = "cv_file_path")
    private String cvFilePath;

    @Column(name = "cv_original_filename")
    private String cvOriginalFilename;

    @Column(name = "cv_file_type")
    private String cvFileType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private UserEntity createdBy;

    @ManyToMany
    @JoinTable(
            name= "candidate_skills",
            joinColumns = @JoinColumn(name= "candidateId"),
            inverseJoinColumns = @JoinColumn(name="skillId")
    )
    @Builder.Default
    private Set<SkillEntity> skills= new HashSet<>();

    @ManyToMany
    @JoinTable(
            name="candidate_tags",
            joinColumns = @JoinColumn(name= "candidateId"),
            inverseJoinColumns = @JoinColumn(name= "tagId")
    )
    @Builder.Default
    private Set<TagEntity> tags= new HashSet<>();
}
