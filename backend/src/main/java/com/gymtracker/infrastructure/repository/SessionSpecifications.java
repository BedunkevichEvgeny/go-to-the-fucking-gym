package com.gymtracker.infrastructure.repository;

import com.gymtracker.domain.ExerciseEntry;
import com.gymtracker.domain.LoggedSession;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

public final class SessionSpecifications {

    private SessionSpecifications() {
    }

    public static Specification<LoggedSession> forUser(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    public static Specification<LoggedSession> dateFrom(LocalDate dateFrom) {
        return (root, query, cb) -> dateFrom == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("sessionDate"), dateFrom);
    }

    public static Specification<LoggedSession> dateTo(LocalDate dateTo) {
        return (root, query, cb) -> dateTo == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("sessionDate"), dateTo);
    }

    public static Specification<LoggedSession> exerciseNameContains(String exerciseName) {
        return (root, query, cb) -> {
            if (exerciseName == null || exerciseName.isBlank()) {
                return cb.conjunction();
            }
            query.distinct(true);
            Join<LoggedSession, ExerciseEntry> join = root.join("exerciseEntries");
            return cb.like(cb.lower(join.get("exerciseNameSnapshot")), "%" + exerciseName.toLowerCase() + "%");
        };
    }
}

