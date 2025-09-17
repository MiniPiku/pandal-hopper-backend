package org.minipiku.pandalhopperv2.Service;

import lombok.RequiredArgsConstructor;
import org.minipiku.pandalhopperv2.DTOs.RouteDTOs.PointDTO;
import org.minipiku.pandalhopperv2.DTOs.RouteDTOs.RouteRequestDTO;
import org.minipiku.pandalhopperv2.DTOs.RouteDTOs.RouteResponseDTO;
import org.minipiku.pandalhopperv2.Utility.TSPSolver;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoutingServiceImpl implements RoutingService {

    private final TSPSolver tspSolver;

    @Override
    public RouteResponseDTO findOptimalRoute(RouteRequestDTO request) {
        // Merge start + pandals into one list
        List<PointDTO> allPoints = new ArrayList<>();
        allPoints.add(request.getStartPoint());  // metro
        allPoints.addAll(request.getPandals()); // pandals

        // Solve TSP (starting from metro)
        List<PointDTO> ordered = tspSolver.solveTSP(allPoints);

        // Build response
        PointDTO origin = ordered.get(0);
        PointDTO destination = ordered.get(ordered.size() - 1);

        List<PointDTO> waypoints = new ArrayList<>();
        if (ordered.size() > 2) {
            waypoints = ordered.subList(1, ordered.size() - 1);
        }

        return new RouteResponseDTO(origin, destination, waypoints);
    }
}