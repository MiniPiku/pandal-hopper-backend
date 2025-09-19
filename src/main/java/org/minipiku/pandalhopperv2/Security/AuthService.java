package org.minipiku.pandalhopperv2.Security;

import lombok.RequiredArgsConstructor;
import org.minipiku.pandalhopperv2.DTOs.AuthDTO.LoginRequestDTO;
import org.minipiku.pandalhopperv2.DTOs.AuthDTO.LoginResponseDTO;
import org.minipiku.pandalhopperv2.DTOs.AuthDTO.SignupRequestDTO;
import org.minipiku.pandalhopperv2.DTOs.AuthDTO.SignupResponseDTO;
import org.minipiku.pandalhopperv2.Entity.User;
import org.minipiku.pandalhopperv2.Repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AuthUtil authUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    //Login method
    public LoginResponseDTO login(LoginRequestDTO loginRequestDTO) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequestDTO.getUsername(), loginRequestDTO.getPassword())
        );
        User user = (User) authentication.getPrincipal();

        String token = authUtil.generateJwtToken(user);
        return new LoginResponseDTO(token, user.getId());
    }

    //Signup method
    public SignupResponseDTO signup(SignupRequestDTO signupRequestDTO) {
        User user = userRepository.findByUsername(signupRequestDTO.getUsername()).orElse(null);

        if (user != null) throw new RuntimeException("Username is already in use");

            user = userRepository.save(User.builder()
                        .username(signupRequestDTO.getUsername())
                        .password(passwordEncoder.encode(signupRequestDTO.getPassword()))
                        .build()
         );
         return new SignupResponseDTO(user.getId(),  user.getUsername(), user.getEmail(), user.getPassword());
    }
}
