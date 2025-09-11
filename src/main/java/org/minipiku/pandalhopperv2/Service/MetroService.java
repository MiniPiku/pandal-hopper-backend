package org.minipiku.pandalhopperv2.Service;

import lombok.RequiredArgsConstructor;
import org.minipiku.pandalhopperv2.Entity.MetroStation;
import org.minipiku.pandalhopperv2.Repository.MetroStationRepository;
import org.minipiku.pandalhopperv2.Utility.Haversine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class MetroService {
    @Autowired
    private MetroStationRepository metroRepo;

    public MetroStation findNearest(double userLat, double userLon) {
        List<MetroStation> stations = metroRepo.findAll();
        MetroStation nearest = null;
        double minDist = Double.MAX_VALUE;

        for (MetroStation s : stations) {
            double dist = Haversine.distance(userLat, userLon, s.getMetroLat(), s.getMetroLon());
            if (dist < minDist) {
                minDist = dist;
                nearest = s;
            }
        }
        return nearest;
    }
}