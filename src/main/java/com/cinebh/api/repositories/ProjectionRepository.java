package com.cinebh.api.repositories;

import com.cinebh.api.entities.Projection;
import com.cinebh.api.repositories.custom.ProjectionQueryRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProjectionRepository extends JpaRepository<Projection, UUID>, ProjectionQueryRepository {
}
