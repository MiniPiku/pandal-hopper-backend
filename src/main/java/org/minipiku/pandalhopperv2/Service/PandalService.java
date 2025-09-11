package org.minipiku.pandalhopperv2.Service;

import lombok.RequiredArgsConstructor;
import org.minipiku.pandalhopperv2.Entity.MetroStation;
import org.minipiku.pandalhopperv2.Entity.Pandal;
import org.minipiku.pandalhopperv2.Repository.PandalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PandalService {
    @Autowired
    private PandalRepository pandalRepo;

    public Map<MetroStation, List<Pandal>> getPandalsByZoneClustered(String zone) {
        List<Pandal> pandals = pandalRepo.findByZone(zone);

        return pandals.stream()
                .filter(p -> p.getMetroStation() != null)
                .collect(Collectors.groupingBy(Pandal::getMetroStation));
    }
}