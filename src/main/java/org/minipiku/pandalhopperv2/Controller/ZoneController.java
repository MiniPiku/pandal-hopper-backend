package org.minipiku.pandalhopperv2.Controller;

import lombok.RequiredArgsConstructor;
import org.minipiku.pandalhopperv2.DTOs.MetrobyZoneDTO;
import org.minipiku.pandalhopperv2.DTOs.PandalbyMetroDTO;
import org.minipiku.pandalhopperv2.Entity.MetroStation;
import org.minipiku.pandalhopperv2.Entity.Pandal;
import org.minipiku.pandalhopperv2.Service.PandalService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/zone")
@RequiredArgsConstructor
public class ZoneController {

    private final PandalService pandalService;

    @GetMapping("/{zone}/metros")
    public List<MetroStation> getMetrosForZone(@PathVariable String zone) {
        return pandalService.getMetrosForZone(zone);
    }
    @GetMapping("/{zone}/metro/{metroId}/pandals")
    public List<Pandal> getPandalsByZoneAndMetro(
            @PathVariable String zone,
            @PathVariable Long metroId) {
        return pandalService.getPandalsByZoneAndMetro(zone, metroId);
    }

    // lightweight metros for a zone (used to place metro markers)
    @GetMapping("/{zone}/metros/simple")
    public List<MetrobyZoneDTO> getMetrosForZoneSimple(@PathVariable String zone) {
        return pandalService.getMetrosForZoneDTO(zone);
    }

    // lightweight pandals for the selected metro (used to drop pandal markers)
    @GetMapping("/{zone}/metro/{metroId}/pandals/simple")
    public List<PandalbyMetroDTO> getPandalsByZoneAndMetroSimple(
            @PathVariable String zone,
            @PathVariable Long metroId) {
        return pandalService.getPandalsByZoneAndMetroDTO(zone, metroId);
    }
}
