package com.banquemisr.recruitment.data.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name="tags")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class TagEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name="id", length = 36)
    private String tagId;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @ManyToMany(mappedBy = "tags")
    private Set<CandidateEntity> candidates= new HashSet<>();
}
