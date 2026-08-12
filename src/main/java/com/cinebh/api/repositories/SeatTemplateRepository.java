package com.cinebh.api.repositories;

import com.cinebh.api.entities.SeatTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SeatTemplateRepository extends JpaRepository<SeatTemplate, UUID> {
}
