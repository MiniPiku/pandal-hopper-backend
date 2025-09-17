package org.minipiku.pandalhopperv2.DTOs.RouteDTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteRequestDTO {
    private PointDTO startPoint;         // metro coords
    private List<PointDTO> pandals; // all pandal coords

}