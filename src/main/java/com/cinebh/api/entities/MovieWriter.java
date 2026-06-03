package com.cinebh.api.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Entity
@Table(name = "movie_writers")
public class MovieWriter extends MovieCredit {
}
