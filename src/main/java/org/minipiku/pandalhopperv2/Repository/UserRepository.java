package org.minipiku.pandalhopperv2.Repository;

import org.minipiku.pandalhopperv2.Entity.AuthProviderType;
import org.minipiku.pandalhopperv2.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    Optional<User> findByProviderIdAndProviderType(String providerId, AuthProviderType authProviderType);
}
