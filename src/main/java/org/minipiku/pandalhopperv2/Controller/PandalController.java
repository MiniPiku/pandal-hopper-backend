package org.minipiku.pandalhopperv2.Controller;

import org.minipiku.pandalhopperv2.DTOs.SimplePandalDTO;
import org.minipiku.pandalhopperv2.Entity.MetroStation;
import org.minipiku.pandalhopperv2.Entity.Pandal;
import org.minipiku.pandalhopperv2.Service.PandalService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/pandals")
public class PandalController {
    private final PandalService pandalService;

    public PandalController(PandalService pandalService) {
        this.pandalService = pandalService;
    }

    @GetMapping
    public List<SimplePandalDTO> getAllPandals() {
        return pandalService.getAllPandals();
    }

    @GetMapping("/zone/{zone}")
    public Map<MetroStation, List<Pandal>> getPandalsByZone(@PathVariable String zone) {
        return pandalService.getPandalsByZoneClustered(zone);
    }

    @GetMapping("/zone/{zone}/simple")
    public List<SimplePandalDTO> getSimplePandalsByZone(@PathVariable String zone) {
        return pandalService.getPandalsByZone(zone);
    }
}