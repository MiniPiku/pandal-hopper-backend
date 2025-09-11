package org.minipiku.pandalhopperv2.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "metro_station")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetroStation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "metro_id")
    private Long metroId;

    @Column(name = "metro_lat")
    private Double  metroLat;

    @Column(name = "metro_lon")
    private Double metroLon;

    @Column(name = "metro_name")
    private String metroName;

    @Column(name = "metro_station_code")
    private String metroStationCode;

    @Column(name = "metro_line")
    private String metroLine;
}

