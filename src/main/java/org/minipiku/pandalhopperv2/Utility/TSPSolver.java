package org.minipiku.pandalhopperv2.Utility;

import org.minipiku.pandalhopperv2.DTOs.RouteDTO.PointDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class TSPSolver {

    public List<PointDTO> solveTSP(List<PointDTO> points) {
        if (points == null || points.isEmpty()) {
            return Collections.emptyList();
        }

        // Copy list to avoid mutating original
        List<PointDTO> remaining = new ArrayList<>(points);
        List<PointDTO> route = new ArrayList<>();

        // Always start from first point (metro)
        PointDTO current = remaining.remove(0);
        route.add(current);

        // Nearest Neighbor heuristic
        while (!remaining.isEmpty()) {
            PointDTO nearest = findNearest(current, remaining);
            route.add(nearest);
            remaining.remove(nearest);
            current = nearest;
        }

        return route;
    }

    private PointDTO findNearest(PointDTO current, List<PointDTO> candidates) {
        PointDTO nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (PointDTO candidate : candidates) {
            double dist = haversine(
                    current.getLat(), current.getLon(),
                    candidate.getLat(), candidate.getLon()
            );
            if (dist < minDistance) {
                minDistance = dist;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}