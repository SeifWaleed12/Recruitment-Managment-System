package com.banquemisr.recruitment.data.specification;

import com.banquemisr.recruitment.data.entity.ApplicationEntity;
import com.banquemisr.recruitment.data.entity.CandidateEntity;
import com.banquemisr.recruitment.data.entity.SkillEntity;
import com.banquemisr.recruitment.data.entity.TagEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class CandidateSpecification {

    private CandidateSpecification() {
    }

    public static Specification<CandidateEntity> build(CandidateSearchCriteria criteria) {
        return (root, query, cb) -> {
            if (criteria == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            // 1. Free-text name search (firstName, lastName, or combined)
            if (criteria.getName() != null && !criteria.getName().trim().isEmpty()) {
                String term = "%" + criteria.getName().trim().toLowerCase() + "%";
                Predicate firstNameMatch = cb.like(cb.lower(root.get("firstName")), term);
                Predicate lastNameMatch = cb.like(cb.lower(root.get("lastName")), term);
                Predicate fullNameMatch = cb.like(
                        cb.lower(cb.concat(cb.concat(root.get("firstName"), " "), root.get("lastName"))),
                        term
                );
                predicates.add(cb.or(firstNameMatch, lastNameMatch, fullNameMatch));
            }

            // 2. Skill Filter
            if (criteria.getSkills() != null && !criteria.getSkills().isEmpty()) {
                List<String> lowerSkills = criteria.getSkills().stream()
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(String::toLowerCase)
                        .toList();

                if (!lowerSkills.isEmpty()) {
                    Join<CandidateEntity, SkillEntity> skillJoin = root.join("skills", JoinType.INNER);
                    predicates.add(cb.lower(skillJoin.get("name")).in(lowerSkills));
                }
            }

            // 3. Tag Filter
            if (criteria.getTags() != null && !criteria.getTags().isEmpty()) {
                List<String> lowerTags = criteria.getTags().stream()
                        .map(String::trim)
                        .filter(t -> !t.isEmpty())
                        .map(String::toLowerCase)
                        .toList();

                if (!lowerTags.isEmpty()) {
                    Join<CandidateEntity, TagEntity> tagJoin = root.join("tags", JoinType.INNER);
                    predicates.add(cb.lower(tagJoin.get("name")).in(lowerTags));
                }
            }

            // 4. Application Status Filter (via Subquery to prevent join explosion)
            if (criteria.getStatus() != null && query != null) {
                Subquery<String> appSubquery = query.subquery(String.class);
                Root<ApplicationEntity> appRoot = appSubquery.from(ApplicationEntity.class);
                appSubquery.select(appRoot.get("candidate").get("candidateId"))
                        .where(
                                cb.equal(appRoot.get("candidate").get("candidateId"), root.get("candidateId")),
                                cb.equal(appRoot.get("status"), criteria.getStatus())
                        );
                predicates.add(cb.exists(appSubquery));
            }

            // 5. Years of Experience Range
            if (criteria.getMinExperience() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("yearsOfExperience"), criteria.getMinExperience()));
            }
            if (criteria.getMaxExperience() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("yearsOfExperience"), criteria.getMaxExperience()));
            }

            if (query != null) {
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
