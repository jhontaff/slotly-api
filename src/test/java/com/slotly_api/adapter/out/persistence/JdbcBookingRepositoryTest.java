package com.slotly_api.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.slotly_api.TestcontainersConfiguration;
import com.slotly_api.domain.Booking;
import com.slotly_api.domain.BookingRepository;
import com.slotly_api.domain.BookingRequest;
import com.slotly_api.domain.exceptions.SlotUnavailableException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class JdbcBookingRepositoryTest {

    @Autowired
    private BookingRepository bookings;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID businessId;
    private UUID providerId;
    private Instant tenAm;

    @BeforeEach
    void createProvider() {
        businessId = UUID.randomUUID();
        providerId = UUID.randomUUID();
        tenAm = Instant.now().plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.HOURS);
        jdbcTemplate.update("insert into provider (id, business_id) values (?, ?)", providerId, businessId);
    }

    private Booking booking(int startMinute, int durationMinutes) {
        BookingRequest request = new BookingRequest(
                businessId, providerId, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID().toString(),
                tenAm.plus(Duration.ofMinutes(startMinute)),
                Duration.ofMinutes(durationMinutes));
        return Booking.create(request, Clock.systemUTC());
    }

    @Test
    void constraintRejectsPartialOverlapWithoutThePreviousCheck() {
        bookings.save(booking(0, 45));
        Booking overlapping = booking(30, 30);

        assertThatThrownBy(() -> bookings.save(overlapping))
                .isInstanceOf(SlotUnavailableException.class);
    }

    @Test
    void constraintAcceptsConsecutiveBookings() {
        bookings.save(booking(0, 30));
        bookings.save(booking(30, 30));

        Integer saved = jdbcTemplate.queryForObject(
                "select count(*) from booking where provider_id = ?", Integer.class, providerId);
        assertThat(saved).isEqualTo(2);
    }
}