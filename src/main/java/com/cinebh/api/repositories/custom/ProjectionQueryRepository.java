package com.cinebh.api.repositories.custom;

import com.cinebh.api.entities.Projection;

import java.util.Optional;
import java.util.UUID;

public interface ProjectionQueryRepository {

    Optional<Projection> findByIdWithDetails(UUID id);
}
