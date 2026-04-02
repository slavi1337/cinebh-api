package com.cinebh.api.repositories;

import com.cinebh.api.entities.Venue;
import com.cinebh.api.repositories.projections.VenueCardProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VenueRepository extends JpaRepository<Venue, UUID> {

    @Query(value = """
            SELECT
                v.id AS id,
                v.name AS name,
                CONCAT(v.street_address, ', ', c.name) AS address,
                v.image_url AS imageUrl
            FROM venues v
            JOIN cities c ON c.id = v.city_id
            ORDER BY v.name ASC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM venues
            """,
            nativeQuery = true)
    Page<VenueCardProjection> findAllHomepageVenues(Pageable pageable);
}
