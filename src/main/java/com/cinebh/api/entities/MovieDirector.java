package com.cinebh.api.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Entity
@Table(name = "movie_directors")
public class MovieDirector extends MovieCredit {
}
