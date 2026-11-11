package com.slotly_api.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.slotly_api.TestcontainersConfiguration;
import com.slotly_api.domain.Booking;
import com.slotly_api.domain.BookingRequest;
import com.slotly_api.domain.exceptions.IdempotencyKeyReusedException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class IdempotentBookingTest {

    private static final int ATTEMPTS = 20;

    @Autowired
    private BookSlotUseCase bookSlot;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID providerId;
    private BookingRequest request;

    @BeforeEach
    void createProviderAndRequest() {
        UUID businessId = UUID.randomUUID();
        providerId = UUID.randomUUID();
        Instant tomorrow = Instant.now().plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.HOURS);
        jdbcTemplate.update("insert into provider (id, business_id) values (?, ?)", providerId, businessId);
        request = new BookingRequest(
                businessId, providerId, UUID.randomUUID(), UUID.randomUUID(),
                "retry-key", tomorrow, Duration.ofMinutes(30));
    }

    private Integer savedBookings() {
        return jdbcTemplate.queryForObject(
                "select count(*) from booking where provider_id = ?", Integer.class, providerId);
    }

    @Test
    void retryingTheSameRequestReturnsTheOriginalBooking() {
        Booking first = bookSlot.book(request);
        Booking retry = bookSlot.book(request);

        assertThat(retry.getId()).isEqualTo(first.getId());
        assertThat(savedBookings()).isEqualTo(1);
    }

    @Test
    void reusingTheKeyForADifferentRequestIsRejected() {
        bookSlot.book(request);
        BookingRequest differentTime = new BookingRequest(
                request.businessId(), request.providerId(), request.customerId(), request.offeringId(),
                request.idempotencyKey(), request.start().plus(Duration.ofHours(2)), request.duration());

        assertThatThrownBy(() -> bookSlot.book(differentTime))
                .isInstanceOf(IdempotencyKeyReusedException.class);
        assertThat(savedBookings()).isEqualTo(1);
    }

    @Test
    void simultaneousRetriesCreateOneBookingAndAllGetItsId() throws Exception {
        CountDownLatch allReady = new CountDownLatch(ATTEMPTS);
        CountDownLatch startSignal = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(ATTEMPTS);

        try {
            List<Future<UUID>> attempts = new ArrayList<>();
            for (int i = 0; i < ATTEMPTS; i++) {
                attempts.add(pool.submit(() -> {
                    allReady.countDown();
                    startSignal.await();
                    return bookSlot.book(request).getId();
                }));
            }

            allReady.await(10, TimeUnit.SECONDS);
            startSignal.countDown();

            Set<UUID> ids = new HashSet<>();
            for (Future<UUID> attempt : attempts) {
                ids.add(attempt.get(30, TimeUnit.SECONDS));
            }

            assertThat(savedBookings()).isEqualTo(1);
            assertThat(ids).hasSize(1);
        } finally {
            pool.shutdownNow();
        }
    }
}