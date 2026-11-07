package com.slotly_api.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.slotly_api.TestcontainersConfiguration;
import com.slotly_api.domain.BookingRequest;
import com.slotly_api.domain.exceptions.SlotUnavailableException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class BookSlotRaceTest {

    private static final int ATTEMPTS = 20;



    @Autowired
    private BookSlotUseCase bookSlot;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void onlyOneOfManySimultaneousBookingsForTheSameSlotSucceeds() throws Exception {
        // Arrange: same business, provider and time for everyone
        UUID businessId = UUID.randomUUID();
        UUID providerId = UUID.randomUUID();
        UUID offeringId = UUID.randomUUID();
        jdbcTemplate.update("insert into provider (id, business_id) values (?, ?)", providerId, businessId);
        Instant start = Instant.now().plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.MINUTES);

        CountDownLatch allReady = new CountDownLatch(ATTEMPTS);
        CountDownLatch startSignal = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(ATTEMPTS);

        try {
            List<Future<Boolean>> attempts = new ArrayList<>();
            for (int i = 0; i < ATTEMPTS; i++) {
                BookingRequest request = new BookingRequest(
                        businessId, providerId, UUID.randomUUID(), offeringId,
                        "race-" + i, start, Duration.ofMinutes(30));

                attempts.add(pool.submit(() -> {
                    allReady.countDown();
                    startSignal.await();
                    try {
                        bookSlot.book(request);
                        return true;
                    } catch (SlotUnavailableException e) {
                        return false;
                    }
                }));
            }

            allReady.await(10, TimeUnit.SECONDS);
            startSignal.countDown();

            int succeeded = 0;
            for (Future<Boolean> attempt : attempts) {
                if (attempt.get(30, TimeUnit.SECONDS)) {
                    succeeded++;
                }
            }

            // Assert: the database is the source of truth
            Integer saved = jdbcTemplate.queryForObject(
                    "select count(*) from booking where provider_id = ?", Integer.class, providerId);

            assertThat(saved).as("bookings saved for the same slot").isEqualTo(1);
            assertThat(succeeded).as("attempts reported as successful").isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }
}