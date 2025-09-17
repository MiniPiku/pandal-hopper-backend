package org.minipiku.pandalhopperv2.DTOs.MetroPandalDTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PandalbyMetroDTO {
    private String name;
    private double latitude;
    private double longitude;
}
