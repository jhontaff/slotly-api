package com.slotly_api.adapter.out.persistence;

import com.slotly_api.domain.Booking;
import com.slotly_api.domain.BookingRepository;
import com.slotly_api.domain.TimeRange;
import com.slotly_api.domain.exceptions.SlotUnavailableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Repository
public class JdbcBookingRepository implements BookingRepository {

    private final JdbcClient jdbc;
    private static final String EXCLUSION_VIOLATION = "23P01";

    JdbcBookingRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }


    @Override
    public boolean existsOverlapping(UUID providerId, TimeRange range) {
        Boolean exists = jdbc.sql("""
                select exists (
                    select 1 from booking
                    where provider_id = :providerId
                      and status <> 'CANCELLED'
                      and start_at < :end
                      and end_at > :start
                )
                """)
                .param("providerId", providerId)
                .param("start", toUtc(range.start()))
                .param("end", toUtc(range.end()))
                .query(Boolean.class)
                .single();
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public void save(Booking booking) {
        try {
            jdbc.sql("""
                insert into booking (id, business_id, provider_id, customer_id, offering_id,
                                     idempotency_key, start_at, end_at, status)
                values (:id, :businessId, :providerId, :customerId, :offeringId,
                        :idempotencyKey, :start, :end, :status)
                """)
                    .param("id", booking.getId())
                    .param("businessId", booking.getBusinessId())
                    .param("providerId", booking.getProviderId())
                    .param("customerId", booking.getCustomerId())
                    .param("offeringId", booking.getOfferingId())
                    .param("idempotencyKey", booking.getIdempotencyKey())
                    .param("start", toUtc(booking.getTimeRange().start()))
                    .param("end", toUtc(booking.getTimeRange().end()))
                    .param("status", booking.getStatus().name())
                    .update();
        } catch (DataIntegrityViolationException e) {
            if (isOverlapViolation(e)) {
                throw new SlotUnavailableException("The slot is no longer available");
            }
            throw e;
        }
    }

    private static boolean isOverlapViolation(DataIntegrityViolationException e) {
        return e.getMostSpecificCause() instanceof SQLException sql
                && EXCLUSION_VIOLATION.equals(sql.getSQLState());
    }

    private static OffsetDateTime toUtc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

}
