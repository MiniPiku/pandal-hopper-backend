package org.minipiku.pandalhopperv2.DTOs.MetroPandalDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimplePandalDTO {
    private String name;
    private double latitude;
    private double longitude;
}
