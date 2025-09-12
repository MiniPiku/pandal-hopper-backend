package org.minipiku.pandalhopperv2.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SimplePandalDTO {
    private String name;
    private double latitude;
    private double longitude;
}
