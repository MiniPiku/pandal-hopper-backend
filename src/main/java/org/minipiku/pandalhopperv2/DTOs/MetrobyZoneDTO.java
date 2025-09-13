package org.minipiku.pandalhopperv2.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MetrobyZoneDTO {
    private Long metroId;
    private String metroName;
    private double metroLat;
    private double metroLon;
}

