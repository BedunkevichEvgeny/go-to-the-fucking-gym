package com.gymtracker.infrastructure.query;

import com.gymtracker.api.dto.ProgressionPoint;
import com.gymtracker.domain.ProgressionMetricType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProgressionQueryBuilder {

    private static final String PROGRESSION_SQL = """
            select ls.id as session_id,
                   ls.session_date as session_date,
                   (
                       select max(ss.weight_value)
                       from strength_sets ss
                       join exercise_entries ees on ees.id = ss.exercise_entry_id
                       where ees.logged_session_id = ls.id
                         and lower(ees.exercise_name_snapshot) like :exercisePattern
                   ) as max_weight,
                   coalesce((
                       select sum(cl.distance_value)
                       from cardio_laps cl
                       join exercise_entries eec on eec.id = cl.exercise_entry_id
                       where eec.logged_session_id = ls.id
                         and lower(eec.exercise_name_snapshot) like :exercisePattern
                   ), 0) as total_distance,
                   coalesce((
                       select sum(cl.duration_seconds)
                       from cardio_laps cl
                       join exercise_entries eec on eec.id = cl.exercise_entry_id
                       where eec.logged_session_id = ls.id
                         and lower(eec.exercise_name_snapshot) like :exercisePattern
                   ), 0) as total_duration
            from logged_sessions ls
            where ls.user_id = :userId
              and exists (
                  select 1
                  from exercise_entries ee
                  where ee.logged_session_id = ls.id
                    and lower(ee.exercise_name_snapshot) like :exercisePattern
              )
            order by ls.session_date asc, ls.created_at asc
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ProgressionQueryBuilder(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ProgressionPoint> fetchProgressionPoints(UUID userId, String exerciseName) {
        String pattern = "%" + exerciseName.toLowerCase(Locale.ROOT) + "%";
        var parameters = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("exercisePattern", pattern);

        return jdbcTemplate.query(PROGRESSION_SQL, parameters, (rs, rowNum) -> {
            UUID sessionId = rs.getObject("session_id", UUID.class);
            LocalDate sessionDate = rs.getObject("session_date", LocalDate.class);
            BigDecimal maxWeight = rs.getBigDecimal("max_weight");
            BigDecimal totalDistance = rs.getBigDecimal("total_distance");
            double distanceValue = totalDistance == null ? 0.0 : totalDistance.doubleValue();
            Number durationNumber = (Number) rs.getObject("total_duration");
            double durationValue = durationNumber == null ? 0.0 : durationNumber.doubleValue();

            if (maxWeight != null) {
                return new ProgressionPoint(sessionId, sessionDate, ProgressionMetricType.WEIGHT, maxWeight.doubleValue());
            }
            if (distanceValue > 0) {
                return new ProgressionPoint(sessionId, sessionDate, ProgressionMetricType.DISTANCE, distanceValue);
            }
            return new ProgressionPoint(sessionId, sessionDate, ProgressionMetricType.DURATION, durationValue);
        });
    }
}

