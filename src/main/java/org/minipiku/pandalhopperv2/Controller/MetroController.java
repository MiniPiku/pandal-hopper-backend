package org.minipiku.pandalhopperv2.Controller;

import lombok.RequiredArgsConstructor;
import org.minipiku.pandalhopperv2.Entity.MetroStation;
import org.minipiku.pandalhopperv2.Service.MetroService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/metro")
public class MetroController {
    private MetroService metroService;

    public MetroController(MetroService metroService) {
        this.metroService = metroService;
    }

    @GetMapping("/nearest")
    public MetroStation getNearestMetro(@RequestParam Double lat, @RequestParam Double lon) {
        return metroService.findNearest(lat, lon);
    }
}
