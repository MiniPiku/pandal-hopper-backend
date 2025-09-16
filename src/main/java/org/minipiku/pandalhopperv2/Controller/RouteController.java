package org.minipiku.pandalhopperv2.Controller;

import lombok.RequiredArgsConstructor;
import org.minipiku.pandalhopperv2.DTOs.RouteRequestDTO;
import org.minipiku.pandalhopperv2.DTOs.RouteResponseDTO;
import org.minipiku.pandalhopperv2.Service.RoutingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/route")
@RequiredArgsConstructor
public class RouteController {

    private final RoutingService routingService;

    @PostMapping("/optimal")
    public ResponseEntity<RouteResponseDTO> getOptimalRoute(@RequestBody RouteRequestDTO request) {
        RouteResponseDTO response = routingService.findOptimalRoute(request);
        return ResponseEntity.ok(response);
    }
}