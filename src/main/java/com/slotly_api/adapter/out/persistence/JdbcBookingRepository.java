package com.slotly_api.adapter.out.persistence;

import com.slotly_api.domain.Booking;
import com.slotly_api.domain.BookingRepository;
import com.slotly_api.domain.BookingStatus;
import com.slotly_api.domain.Cancellation;
import com.slotly_api.domain.TimeRange;
import com.slotly_api.domain.exceptions.BookingNotFoundException;
import com.slotly_api.domain.exceptions.IdempotencyKeyReusedException;
import com.slotly_api.domain.exceptions.SlotUnavailableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcBookingRepository implements BookingRepository {

    private static final String EXCLUSION_VIOLATION = "23P01";

    private static final String PARAM_ID = "id";
    private static final String PARAM_START = "start";
    private static final String PARAM_END = "end";
    private static final String STATUS = "status";
    private static final String UNIQUE_VIOLATION = "23505";
    private static final String IDEMPOTENCY_CONSTRAINT = "uq_booking_idempotency";

    private final JdbcClient jdbc;

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
                .param(PARAM_START, toUtc(range.start()))
                .param(PARAM_END, toUtc(range.end()))
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
                    .param(PARAM_ID, booking.getId())
                    .param("businessId", booking.getBusinessId())
                    .param("providerId", booking.getProviderId())
                    .param("customerId", booking.getCustomerId())
                    .param("offeringId", booking.getOfferingId())
                    .param("idempotencyKey", booking.getIdempotencyKey())
                    .param(PARAM_START, toUtc(booking.getTimeRange().start()))
                    .param(PARAM_END, toUtc(booking.getTimeRange().end()))
                    .param(STATUS, booking.getStatus().name())
                    .update();
        } catch (DataIntegrityViolationException e) {
            throw translate(e);
        }
    }

    @Override
    public Optional<Booking> findById(UUID id) {
        return jdbc.sql("""
                select id, business_id, provider_id, customer_id, offering_id, idempotency_key,
                       start_at, end_at, status, cancelled_at, late_cancellation
                from booking
                where id = :id
                """)
                .param(PARAM_ID, id)
                .query(JdbcBookingRepository::toBooking)
                .optional();
    }

    @Override
    public void update(Booking booking) {
        Optional<Cancellation> cancellation = booking.getCancellation();
        try {
            int updated = jdbc.sql("""
                    update booking
                    set start_at = :start,
                        end_at = :end,
                        status = :status,
                        cancelled_at = :cancelledAt,
                        late_cancellation = :late
                    where id = :id
                    """)
                    .param(PARAM_ID, booking.getId())
                    .param(PARAM_START, toUtc(booking.getTimeRange().start()))
                    .param(PARAM_END, toUtc(booking.getTimeRange().end()))
                    .param(STATUS, booking.getStatus().name())
                    .param("cancelledAt",
                            cancellation.map(c -> toUtc(c.cancelledAt())).orElse(null),
                            Types.TIMESTAMP_WITH_TIMEZONE)
                    .param("late", cancellation.map(Cancellation::late).orElse(null), Types.BOOLEAN)
                    .update();

            if (updated == 0) {
                throw new BookingNotFoundException(booking.getId());
            }
        } catch (DataIntegrityViolationException e) {
            throw translate(e);
        }
    }

    @Override
    public Optional<Booking> findByIdempotencyKey(UUID businessId, UUID customerId, String idempotencyKey) {
        return jdbc.sql("""
            select id, business_id, provider_id, customer_id, offering_id, idempotency_key,
                   start_at, end_at, status, cancelled_at, late_cancellation
            from booking
            where business_id = :businessId
              and customer_id = :customerId
              and idempotency_key = :idempotencyKey
            """)
                .param("businessId", businessId)
                .param("customerId", customerId)
                .param("idempotencyKey", idempotencyKey)
                .query(JdbcBookingRepository::toBooking)
                .optional();
    }



    private static Booking toBooking(ResultSet rs, int rowNum) throws SQLException {
        OffsetDateTime cancelledAt = rs.getObject("cancelled_at", OffsetDateTime.class);
        Cancellation cancellation = cancelledAt == null
                ? null
                : new Cancellation(cancelledAt.toInstant(),
                        Boolean.TRUE.equals(rs.getObject("late_cancellation", Boolean.class)));

        return Booking.restore(
                rs.getObject("id", UUID.class),
                rs.getObject("business_id", UUID.class),
                rs.getObject("provider_id", UUID.class),
                rs.getObject("customer_id", UUID.class),
                rs.getObject("offering_id", UUID.class),
                rs.getString("idempotency_key"),
                new TimeRange(
                        requiredInstant(rs, "start_at"),
                        requiredInstant(rs, "end_at")),
                BookingStatus.valueOf(rs.getString(STATUS)),
                cancellation);
    }

    private static Instant requiredInstant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return Objects.requireNonNull(value, column + " must not be null").toInstant();
    }

    private static RuntimeException translate(DataIntegrityViolationException e) {
        if (isOverlapViolation(e)) {
            return new SlotUnavailableException("The slot is no longer available");
        }
        if (isIdempotencyViolation(e)) {
            return new IdempotencyKeyReusedException();
        }
        return e;
    }

    private static boolean isIdempotencyViolation(DataIntegrityViolationException e) {
        return e.getMostSpecificCause() instanceof SQLException sql
                && UNIQUE_VIOLATION.equals(sql.getSQLState())
                && String.valueOf(sql.getMessage()).contains(IDEMPOTENCY_CONSTRAINT);
    }

    private static boolean isOverlapViolation(DataIntegrityViolationException e) {
        return e.getMostSpecificCause() instanceof SQLException sql
                && EXCLUSION_VIOLATION.equals(sql.getSQLState());
    }

    private static OffsetDateTime toUtc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
