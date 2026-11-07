package com.slotly_api.domain;

import java.util.UUID;

public interface ProviderRepository {

    void lockForUpdate(UUID providerId);
}