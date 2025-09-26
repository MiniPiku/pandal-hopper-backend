package org.minipiku.pandalhopperv2.Service;

import org.springframework.cache.annotation.Cacheable;
import lombok.RequiredArgsConstructor;
import org.minipiku.pandalhopperv2.DTOs.MetroPandalDTO.MetrobyZoneDTO;
import org.minipiku.pandalhopperv2.DTOs.MetroPandalDTO.PandalbyMetroDTO;
import org.minipiku.pandalhopperv2.DTOs.MetroPandalDTO.SimplePandalDTO;
import org.minipiku.pandalhopperv2.Entity.MetroStation;
import org.minipiku.pandalhopperv2.Entity.Pandal;
import org.minipiku.pandalhopperv2.Repository.PandalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PandalService {

    /**
     * Repository for accessing Pandal-related database operations.
     * Spring will auto-wire this dependency.
     */
    @Autowired
    private PandalRepository pandalRepo;

    /**
     * Returns a map of MetroStation to a list of Pandals
     * for a given zone, grouped by their nearest metro.
     *
     * @param zone the name of the zone to filter Pandals
     * @return Map<MetroStation, List<Pandal>> grouped by MetroStation
     */
    public Map<MetroStation, List<Pandal>> getPandalsByZoneClustered(String zone) {
        List<Pandal> pandals = pandalRepo.findByZone(zone);
        System.out.println("🚨 Fetching from DB (not cache)");
        return pandals.stream()
                .filter(p -> p.getMetroStation() != null)
                .collect(Collectors.groupingBy(Pandal::getMetroStation));
    }

    /**
     * Returns a cached list of simplified Pandals for a given zone.
     * If the result is not in cache, fetches from DB and caches it.
     *
     * @param zone the name of the zone to filter Pandals
     * @return List<SimplePandalDTO> containing minimal Pandal details
     */
    @Cacheable(
            value = "pandalsByZone",
            key = "#zone"
    )
    public List<SimplePandalDTO> getPandalsByZone(String zone) {
        System.out.println("🚨 Fetching from DB (not cache)");
        return pandalRepo.findByZone(zone)
                .stream()
                .map(p -> new SimplePandalDTO(
                        p.getName(),
                        p.getLatitude(),
                        p.getLongitude())
                )
                .toList();
    }

    /**
     * Returns a cached list of all Pandals across all zones.
     * Useful for global listings or map visualizations.
     *
     * @return List<SimplePandalDTO> containing all Pandals
     */
    @Cacheable(value = "allPandals")
    public List<SimplePandalDTO> getAllPandals() {
        System.out.println("🚨 Fetching from DB (not cache)");
        return pandalRepo.findAll()
                .stream()
                .map(p -> new SimplePandalDTO(
                        p.getName(),
                        p.getLatitude(),
                        p.getLongitude()))
                .toList();
    }

    /**
     * Fetches all distinct MetroStations present in a given zone.
     *
     * @param zone the zone name
     * @return List<MetroStation> distinct metro stations
     */
    @Cacheable(
            value = "metrosForZone",
            key = "#zone"
    )
    public List<MetroStation> getMetrosForZone(String zone) {
        System.out.println("🚨 Fetching from DB (not cache)");
        return pandalRepo.findDistinctMetrosByZone(zone);
    }

    /**
     * Returns all Pandals in a specific zone and metro station.
     *
     * @param zone    the zone name
     * @param metroId the metro station ID
     * @return List<Pandal> filtered by zone and metro
     */
    @Cacheable(
            value = "pandalsByZoneAndMetro",
            key = "#zone + '-' + #metroId"
    )
    public List<Pandal> getPandalsByZoneAndMetro(String zone, Long metroId) {
        System.out.println("🚨 Fetching from DB (not cache)");
        return pandalRepo.findByZoneAndMetroStation_MetroId(zone, metroId);
    }

    /**
     * Converts MetroStation entities to MetrobyZoneDTO for API responses.
     *
     * @param zone the zone name
     * @return List<MetrobyZoneDTO> simplified metro station details
     */
    @Cacheable(
            value = "metrosForZoneDTO",
            key = "#zone"
    )

    public List<MetrobyZoneDTO> getMetrosForZoneDTO(String zone) {
        System.out.println("🚨 Fetching from DB (not cache)");
        return getMetrosForZone(zone).stream()
                .map(m -> new MetrobyZoneDTO(
                        m.getMetroId(),
                        m.getMetroName(),
                        m.getMetroLat(),
                        m.getMetroLon()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Converts Pandals for a given zone and metro into PandalbyMetroDTO
     * for lightweight API responses.
     *
     * @param zone    the zone name
     * @param metroId the metro station ID
     * @return List<PandalbyMetroDTO> simplified pandal details
     */
    @Cacheable(
            value = "pandalsByZoneAndMetroDTO",
            key = "#zone + '-' + #metroId"
    )
    public List<PandalbyMetroDTO> getPandalsByZoneAndMetroDTO(String zone, Long metroId) {
        System.out.println("🚨 Fetching from DB (not cache)");
        return getPandalsByZoneAndMetro(zone, metroId).stream()
                .map(p -> new PandalbyMetroDTO(
                        p.getName(),
                        p.getLatitude(),
                        p.getLongitude()
                ))
                .collect(Collectors.toList());
    }

}
