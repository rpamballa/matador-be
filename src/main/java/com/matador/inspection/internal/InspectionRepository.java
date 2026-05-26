package com.matador.inspection.internal;

import com.matador.inspection.InspectionEnums.Phase;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InspectionRepository extends JpaRepository<Inspection, UUID> {

    boolean existsByTripIdAndPhase(UUID tripId, Phase phase);

    List<Inspection> findByTripId(UUID tripId);
}
