package org.minipiku.pandalhopperv2.Controller;

import lombok.RequiredArgsConstructor;
import org.minipiku.pandalhopperv2.DTOs.AuthDTO.LoginRequestDTO;
import org.minipiku.pandalhopperv2.DTOs.AuthDTO.LoginResponseDTO;
import org.minipiku.pandalhopperv2.DTOs.AuthDTO.SignupRequestDTO;
import org.minipiku.pandalhopperv2.DTOs.AuthDTO.SignupResponseDTO;
import org.minipiku.pandalhopperv2.Security.AuthService;
import org.minipiku.pandalhopperv2.Security.OAuth2CodeExchangeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// CORS is configured centrally in WebSecurityConfig against an origin
// allowlist; the previous @CrossOrigin("*") here could not be narrowed.
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OAuth2CodeExchangeService codeExchangeService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO loginRequestDTO) {
        return ResponseEntity.ok(authService.login(loginRequestDTO));
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponseDTO> signup(@RequestBody SignupRequestDTO signupRequestDTO) {
        return ResponseEntity.ok(authService.signup(signupRequestDTO));
    }

    /**
     * Exchanges the single-use code from the OAuth2 redirect for a JWT.
     * Called by the frontend callback page; the code is valid for 60 seconds
     * and is destroyed on first redemption.
     */
    @PostMapping("/exchange")
    public ResponseEntity<?> exchange(@RequestBody Map<String, String> body) {
        LoginResponseDTO login = codeExchangeService.redeemCode(body.get("code"));

        if (login == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", 401,
                    "error", "Unauthorized",
                    "message", "Invalid or expired code"
            ));
        }

        return ResponseEntity.ok(login);
    }
}
