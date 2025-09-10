package org.minipiku.pandalhopperv2.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pandals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pandal {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String pandalId;

    private Double latitude;
    private Double longitude;
    private String zone;
    private String city;
    private String name;
    private String address;
    private Double searchScore;
    private Double metroDistance;
    private String metroDistanceUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metro_id")
    private MetroStation metroStation;
}