package com.matador.inspection.internal;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InspectionPhotoRepository extends JpaRepository<InspectionPhoto, UUID> {

    List<InspectionPhoto> findByInspectionId(UUID inspectionId);
}
