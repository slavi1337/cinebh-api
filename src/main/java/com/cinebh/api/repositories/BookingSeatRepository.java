package com.cinebh.api.repositories;

import com.cinebh.api.entities.BookingSeat;
import com.cinebh.api.repositories.custom.BookingSeatQueryRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BookingSeatRepository extends JpaRepository<BookingSeat, UUID>, BookingSeatQueryRepository {
}
