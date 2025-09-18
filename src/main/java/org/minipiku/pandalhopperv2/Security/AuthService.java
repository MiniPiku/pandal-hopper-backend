package org.minipiku.pandalhopperv2.Security;

import lombok.RequiredArgsConstructor;
import org.minipiku.pandalhopperv2.DTOs.AuthDTO.LoginRequestDTO;
import org.minipiku.pandalhopperv2.DTOs.AuthDTO.LoginResponseDTO;
import org.minipiku.pandalhopperv2.Entity.User;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AuthUtil authUtil;

    public LoginResponseDTO login(LoginRequestDTO loginRequestDTO) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequestDTO.getUserName(), loginRequestDTO.getPassword())
        );
        User user = (User) authentication.getPrincipal();

        String token = authUtil.generateJwtToken(user);
        return new LoginResponseDTO(token, user.getId());
    }
}
