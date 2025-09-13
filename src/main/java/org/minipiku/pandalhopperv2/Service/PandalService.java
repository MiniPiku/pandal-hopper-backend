package org.minipiku.pandalhopperv2.Service;

import lombok.RequiredArgsConstructor;
import org.minipiku.pandalhopperv2.DTOs.MetrobyZoneDTO;
import org.minipiku.pandalhopperv2.DTOs.PandalbyMetroDTO;
import org.minipiku.pandalhopperv2.DTOs.SimplePandalDTO;
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
    @Autowired
    private PandalRepository pandalRepo;

    public Map<MetroStation, List<Pandal>> getPandalsByZoneClustered(String zone) {
        List<Pandal> pandals = pandalRepo.findByZone(zone);

        return pandals.stream()
                .filter(p -> p.getMetroStation() != null)
                .collect(Collectors.groupingBy(Pandal::getMetroStation));
    }
    public List<SimplePandalDTO> getPandalsByZone(String zone) {
        return pandalRepo.findByZone(zone)
                .stream()
                .map(p -> new SimplePandalDTO(
                        p.getName(),
                        p.getLatitude(),
                        p.getLongitude())
                )
                .toList();
    }
    public List<SimplePandalDTO> getAllPandals() {
        return pandalRepo.findAll()
                .stream()
                .map(p -> new SimplePandalDTO(
                        p.getName(),
                        p.getLatitude(),
                        p.getLongitude()))
                .toList();
    }
    public List<MetroStation> getMetrosForZone(String zone) {
        return pandalRepo.findDistinctMetrosByZone(zone);
    }

    public List<Pandal> getPandalsByZoneAndMetro(String zone, Long metroId) {
        return pandalRepo.findByZoneAndMetroStation_MetroId(zone, metroId);
    }

    public List<MetrobyZoneDTO> getMetrosForZoneDTO(String zone) {
        return getMetrosForZone(zone).stream()
                .map(m -> new MetrobyZoneDTO(
                        m.getMetroId(),
                        m.getMetroName(),
                        m.getMetroLat(),
                        m.getMetroLon()
                ))
                .collect(Collectors.toList());
    }

    public List<PandalbyMetroDTO> getPandalsByZoneAndMetroDTO(String zone, Long metroId) {
        return getPandalsByZoneAndMetro(zone, metroId).stream()
                .map(p -> new PandalbyMetroDTO(
                        p.getName(),
                        p.getLatitude(),
                        p.getLongitude()
                ))
                .collect(Collectors.toList());
    }

}