package org.minipiku.pandalhopperv2.Service;

import org.minipiku.pandalhopperv2.DTOs.RouteDTO.RouteRequestDTO;
import org.minipiku.pandalhopperv2.DTOs.RouteDTO.RouteResponseDTO;

public interface RoutingService {
    RouteResponseDTO findOptimalRoute(RouteRequestDTO request);
}