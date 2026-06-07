package com.cinebh.api.repositories;

import com.cinebh.api.entities.Movie;
import com.cinebh.api.repositories.custom.MovieQueryRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MovieRepository extends JpaRepository<Movie, UUID>, MovieQueryRepository {
}
