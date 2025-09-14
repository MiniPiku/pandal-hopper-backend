package org.minipiku.pandalhopperv2.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointDTO {
    private double lat;
    private double lon;
    private String name;
}
