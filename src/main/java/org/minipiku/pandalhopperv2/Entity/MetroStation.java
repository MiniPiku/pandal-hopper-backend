package org.minipiku.pandalhopperv2.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "metro_station")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetroStation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long metroId;

    private Double metroLat;
    private Double metroLon;
    private String metroName;
    private String metroStationCode;
    private String metroLine;
}

