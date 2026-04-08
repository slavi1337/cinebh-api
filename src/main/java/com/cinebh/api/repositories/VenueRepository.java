package com.cinebh.api.repositories;

import com.cinebh.api.entities.Venue;
import com.cinebh.api.repositories.custom.VenueQueryRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VenueRepository extends JpaRepository<Venue, UUID>, VenueQueryRepository {
}
