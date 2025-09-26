package org.minipiku.pandalhopperv2.DTOs.MetroPandalDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PandalDTO {
    private String name;
    private double pandal_latitude;
    private double pandal_longitude;
}
