package org.minipiku.pandalhopperv2.Service;

import org.minipiku.pandalhopperv2.DTOs.RouteDTOs.RouteRequestDTO;
import org.minipiku.pandalhopperv2.DTOs.RouteDTOs.RouteResponseDTO;

public interface RoutingService {
    RouteResponseDTO findOptimalRoute(RouteRequestDTO request);
}