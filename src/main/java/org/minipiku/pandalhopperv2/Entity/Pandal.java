package org.minipiku.pandalhopperv2.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "pandals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pandal {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID pandalId;

    private Double latitude;
    private Double longitude;
    private String zone;
    private String city;
    private String name;
    private String address;

    @Column(name = "search_score")
    private Double searchScore;

    @Column(name = "metro_distance")
    private Double metroDistance;

    @Column(name = "metro_distance_unit")
    private String metroDistanceUnit;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "metro_id")
    private MetroStation metroStation;
}