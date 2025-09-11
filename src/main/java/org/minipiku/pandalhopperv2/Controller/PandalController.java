package org.minipiku.pandalhopperv2.Controller;

import org.minipiku.pandalhopperv2.Entity.MetroStation;
import org.minipiku.pandalhopperv2.Entity.Pandal;
import org.minipiku.pandalhopperv2.Service.PandalService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/pandals")
public class PandalController {
    private final PandalService pandalService;

    public PandalController(PandalService pandalService) {
        this.pandalService = pandalService;
    }

    @GetMapping("/zone/{zone}")
    public Map<MetroStation, List<Pandal>> getPandalsByZone(@PathVariable String zone) {
        return pandalService.getPandalsByZoneClustered(zone);
    }
}