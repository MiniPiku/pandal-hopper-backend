package org.minipiku.pandalhopperv2.DTOs.RouteDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteResponseDTO {
    private PointDTO origin;
    private PointDTO destination;
    private List<PointDTO> waypoints;
}