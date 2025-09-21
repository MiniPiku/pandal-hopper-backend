package org.minipiku.pandalhopperv2.Security;

import lombok.RequiredArgsConstructor;
import org.minipiku.pandalhopperv2.DTOs.AuthDTO.LoginRequestDTO;
import org.minipiku.pandalhopperv2.DTOs.AuthDTO.LoginResponseDTO;
import org.minipiku.pandalhopperv2.DTOs.AuthDTO.SignupRequestDTO;
import org.minipiku.pandalhopperv2.DTOs.AuthDTO.SignupResponseDTO;
import org.minipiku.pandalhopperv2.Entity.AuthProviderType;
import org.minipiku.pandalhopperv2.Entity.User;
import org.minipiku.pandalhopperv2.Repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service

public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AuthUtil authUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            AuthUtil authUtil,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Lazy AuthenticationManager authenticationManager
    ) {
        this.authUtil = authUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    /**
     * ---------------------- LOGIN ----------------------
     * Authenticates the user using username/password and
     * returns a JWT token with the user's ID.
     */
    public LoginResponseDTO login(LoginRequestDTO loginRequestDTO) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequestDTO.getUsername(),
                        loginRequestDTO.getPassword()
                )
        );

        User user = (User) authentication.getPrincipal();
        String token = authUtil.generateJwtToken(user);

        return new LoginResponseDTO(token, user.getId());
    }

    /**
     * ---------------------- SIGNUP (internal) ----------------------
     * Creates a new User entity in the database.
     * Returns the saved User entity.
     *
     * NOTE: This is an internal helper for actual persistence.
     */
    private User signupInternal(LoginRequestDTO signupRequestDTO) {
        // Check if username already exists
        if (userRepository.findByUsername(signupRequestDTO.getUsername()).isPresent()) {
            throw new RuntimeException("Username is already in use");
        }

        // Create and save new user
        User user = User.builder()
                .username(signupRequestDTO.getUsername())
                .password(passwordEncoder.encode(signupRequestDTO.getPassword()))
                .build();

        return userRepository.save(user);
    }

    /**
     * ---------------------- SIGNUP (public) ----------------------
     * Handles the external signup call and returns a DTO
     * that hides sensitive fields like password.
     */
    public SignupResponseDTO signup(SignupRequestDTO signupRequestDTO) {

        if (userRepository.findByUsername(signupRequestDTO.getUsername()).isPresent()) {
            throw new RuntimeException("Username is already in use");
        }

        User user = User.builder()
                .username(signupRequestDTO.getUsername())
                .email(signupRequestDTO.getEmail()) // assuming SignupRequestDTO contains email
                .password(passwordEncoder.encode(signupRequestDTO.getPassword()))
                .build();

        user = userRepository.save(user);

        return new SignupResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                null // ⚠️ Never expose password in response
        );
    }

    /**
     * ---------------------- OAUTH2 LOGIN ----------------------
     * Handles OAuth2 login or first-time signup.
     * If the user doesn't exist, creates a new record.
     */
    public ResponseEntity<LoginResponseDTO> handleOauth2LoginUser(OAuth2User oAuth2User, String registrationId) {
        AuthProviderType providerType = authUtil.getProviderTypeFromRegistrationId(registrationId);
        String providerId = authUtil.determineProviderIdFromOAuth2User(oAuth2User, registrationId);

        // 1. Try to find user by provider ID
        User user = userRepository.findByProviderIdAndProviderType(providerId, providerType).orElse(null);

        // 2. Check if email already exists
        String email = oAuth2User.getAttribute("email");
        User emailUser = (email != null) ? userRepository.findByUsername(email).orElse(null) : null;

        if (user == null && emailUser == null) {
            // First-time signup for OAuth2 user
            String username = authUtil.determineUsernameFromOAuth2User(oAuth2User, registrationId, providerId);

            // Create a new user with no password
            user = User.builder()
                    .username(username)
                    .email(email)
                    .providerId(providerId)
                    .providerType(providerType)
                    .password(null)
                    .build();

            userRepository.save(user);

        } else if (user != null) {
            // Existing provider user -> update email if needed
            if (email != null && !email.isBlank() && !email.equals(user.getUsername())) {
                user.setEmail(email);
                user.setUsername(email); // optional: treat email as username
                userRepository.save(user);
            }

        } else {
            // Email already registered but not linked to this provider
            throw new BadCredentialsException("Email is already registered: " + email);
        }

        // Generate a JWT for the (new or existing) user
        String token = authUtil.generateJwtToken(user);
        return ResponseEntity.ok(new LoginResponseDTO(token, user.getId()));
    }
}
