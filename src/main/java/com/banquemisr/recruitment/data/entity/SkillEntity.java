package com.banquemisr.recruitment.data.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name="skills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class SkillEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name="id", length = 36)
    private String skillId;

    @Column(name="name", nullable = false, unique = true)
    private String name;

    @ManyToMany(mappedBy = "skills")
    private Set<CandidateEntity> candidates= new HashSet<>();
}
