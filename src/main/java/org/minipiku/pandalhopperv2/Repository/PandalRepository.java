package org.minipiku.pandalhopperv2.Repository;

import org.minipiku.pandalhopperv2.Entity.Pandal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PandalRepository extends JpaRepository<Pandal, String> {
    List<Pandal> findByZone(String zone);
}
