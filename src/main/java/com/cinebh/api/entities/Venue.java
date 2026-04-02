package com.cinebh.api.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "venues")
public class Venue {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "street_address", nullable = false)
    private String streetAddress;

    private String phone;

    @Column(name = "image_url")
    private String imageUrl;
}
