package org.minipiku.pandalhopperv2.Controller;

import lombok.RequiredArgsConstructor;
import org.minipiku.pandalhopperv2.DTOs.RouteDTO.RouteRequestDTO;
import org.minipiku.pandalhopperv2.DTOs.RouteDTO.RouteResponseDTO;
import org.minipiku.pandalhopperv2.Service.RoutingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// CORS handled centrally in WebSecurityConfig.
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

    /**
     * Rejected input is a client error. Without this the guards in
     * RoutingServiceImpl would surface as 500s.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", e.getMessage()
        ));
    }
}
