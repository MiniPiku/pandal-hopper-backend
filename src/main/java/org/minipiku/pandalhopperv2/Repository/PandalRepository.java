package org.minipiku.pandalhopperv2.Repository;

import org.minipiku.pandalhopperv2.Entity.MetroStation;
import org.minipiku.pandalhopperv2.Entity.Pandal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PandalRepository extends JpaRepository<Pandal, String> {
    List<Pandal> findByZone(String zone);

    @Query("SELECT DISTINCT p.metroStation FROM Pandal p WHERE p.zone = :zone")
    List<MetroStation> findDistinctMetrosByZone(@Param("zone") String zone);

    List<Pandal> findByZoneAndMetroStation_MetroId(String zone, Long metroId);

}
