package com.slotly_api.adapter.out.persistence;

import com.slotly_api.domain.ProviderRepository;
import com.slotly_api.domain.exceptions.ProviderNotFoundException;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcProviderRepository implements ProviderRepository {

    private final JdbcClient jdbc;

    JdbcProviderRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void lockForUpdate(UUID providerId) {
        boolean found = jdbc.sql("select 1 from provider where id = :id for update")
                .param("id", providerId)
                .query(Integer.class)
                .optional()
                .isPresent();

        if (!found) {
            throw new ProviderNotFoundException(providerId);
        }
    }
}