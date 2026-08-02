package org.minipiku.pandalhopperv2.Service;

import lombok.RequiredArgsConstructor;
import org.minipiku.pandalhopperv2.DTOs.RouteDTO.PointDTO;
import org.minipiku.pandalhopperv2.DTOs.RouteDTO.RouteRequestDTO;
import org.minipiku.pandalhopperv2.DTOs.RouteDTO.RouteResponseDTO;
import org.minipiku.pandalhopperv2.Utility.TSPSolver;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoutingServiceImpl implements RoutingService {

    /**
     * The solver is O(n^2) and this endpoint is unauthenticated, so an
     * unbounded waypoint list is a cheap way to burn CPU. 100 stops is well
     * beyond any realistic day of pandal hopping.
     */
    private static final int MAX_WAYPOINTS = 100;

    private final TSPSolver tspSolver;

    @Override
    public RouteResponseDTO findOptimalRoute(RouteRequestDTO request) {
        if (request == null || request.getStartPoint() == null) {
            throw new IllegalArgumentException("startPoint is required");
        }

        List<PointDTO> pandals = request.getPandals();
        if (pandals == null || pandals.isEmpty()) {
            throw new IllegalArgumentException("At least one pandal is required");
        }
        if (pandals.size() > MAX_WAYPOINTS) {
            throw new IllegalArgumentException(
                    "Too many pandals: " + pandals.size() + " (max " + MAX_WAYPOINTS + ")");
        }
        if (pandals.stream().anyMatch(p -> p == null)) {
            throw new IllegalArgumentException("pandals must not contain null entries");
        }

        // Merge start + pandals into one list
        List<PointDTO> allPoints = new ArrayList<>();
        allPoints.add(request.getStartPoint());  // metro
        allPoints.addAll(pandals);

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