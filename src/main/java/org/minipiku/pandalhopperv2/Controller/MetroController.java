package org.minipiku.pandalhopperv2.Controller;

import org.minipiku.pandalhopperv2.DTOs.MetroPandalDTO.MetroLocationDTO;
import org.minipiku.pandalhopperv2.Entity.MetroStation;
import org.minipiku.pandalhopperv2.Service.MetroService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
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

    @GetMapping("/nearest/location")
    public ResponseEntity<MetroLocationDTO> getNearestMetroLocation(
            @RequestParam double lat,
            @RequestParam double lon) {

        MetroLocationDTO dto = metroService.findNearestMinimal(lat, lon);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }
}
