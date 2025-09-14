package org.minipiku.pandalhopperv2.Service;

import org.minipiku.pandalhopperv2.DTOs.RouteRequestDTO;
import org.minipiku.pandalhopperv2.DTOs.RouteResponseDTO;

public interface RoutingService {
    RouteResponseDTO findOptimalRoute(RouteRequestDTO request);
}