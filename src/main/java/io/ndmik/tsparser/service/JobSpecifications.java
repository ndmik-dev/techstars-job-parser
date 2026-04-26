package io.ndmik.tsparser.service;

import io.ndmik.tsparser.dto.JobFilter;
import io.ndmik.tsparser.model.Company;
import io.ndmik.tsparser.model.Job;
import io.ndmik.tsparser.model.Tag;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public final class JobSpecifications {

    private JobSpecifications() {
    }

    public static Specification<Job> byFilter(JobFilter filter) {
        Specification<Job> specification = active(filter.active());
        specification = and(specification, search(filter.q()));
        specification = and(specification, location(filter.location()));
        specification = and(specification, company(filter.company()));
        specification = and(specification, tag(filter.tag()));
        specification = and(specification, remoteType(filter.remoteType()));
        return and(specification, seniority(filter.seniority()));
    }

    private static Specification<Job> and(Specification<Job> current, Specification<Job> next) {
        return next == null
                ? current
                : current.and(next);
    }

    private static Specification<Job> active(Boolean active) {
        return (root, _, criteriaBuilder) -> active == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("active"), active);
    }

    private static Specification<Job> search(String value) {
        if (!hasText(value)) {
            return null;
        }

        return (root, _, criteriaBuilder) -> {
            Join<Job, Company> company = root.join("company", JoinType.LEFT);
            String pattern = contains(value);

            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(company.get("name")), pattern)
            );
        };
    }

    private static Specification<Job> location(String value) {
        return containsIgnoreCase("location", value);
    }

    private static Specification<Job> remoteType(String value) {
        return containsIgnoreCase("remoteType", value);
    }

    private static Specification<Job> seniority(String value) {
        return containsIgnoreCase("seniority", value);
    }

    private static Specification<Job> company(String value) {
        if (!hasText(value)) {
            return null;
        }

        return (root, _, criteriaBuilder) -> {
            Join<Job, Company> company = root.join("company", JoinType.LEFT);
            return criteriaBuilder.like(criteriaBuilder.lower(company.get("name")), contains(value));
        };
    }

    private static Specification<Job> tag(String value) {
        if (!hasText(value)) {
            return null;
        }

        return (root, query, criteriaBuilder) -> {
            query.distinct(true);
            Join<Job, Tag> tag = root.join("tags", JoinType.LEFT);
            return criteriaBuilder.like(criteriaBuilder.lower(tag.get("name")), contains(value));
        };
    }

    private static Specification<Job> containsIgnoreCase(String field, String value) {
        if (!hasText(value)) {
            return null;
        }

        return (root, _, criteriaBuilder) ->
                criteriaBuilder.like(criteriaBuilder.lower(root.get(field)), contains(value));
    }

    private static String contains(String value) {
        return "%" + value.trim().toLowerCase() + "%";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
