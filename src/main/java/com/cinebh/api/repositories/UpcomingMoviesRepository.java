package com.cinebh.api.repositories;

import com.cinebh.api.entities.Projection;
import com.cinebh.api.repositories.custom.UpcomingMoviesQueryRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UpcomingMoviesRepository extends JpaRepository<Projection, UUID>, UpcomingMoviesQueryRepository {
}
