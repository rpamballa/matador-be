package com.matador.inspection;

import com.matador.inspection.InspectionEnums.Angle;
import com.matador.inspection.InspectionEnums.Phase;
import com.matador.inspection.api.InspectionDtos.InspectionResponse;
import com.matador.inspection.api.InspectionDtos.PhotoSubmission;
import com.matador.inspection.api.InspectionDtos.PhotoView;
import com.matador.inspection.api.InspectionDtos.ReviewRequest;
import com.matador.inspection.api.InspectionDtos.SubmitInspectionRequest;
import com.matador.inspection.api.InspectionDtos.UploadUrlResponse;
import com.matador.inspection.events.InspectionEvents.InspectionSubmitted;
import com.matador.inspection.internal.Inspection;
import com.matador.inspection.internal.InspectionPhoto;
import com.matador.inspection.internal.InspectionPhotoRepository;
import com.matador.inspection.internal.InspectionRepository;
import com.matador.shared.error.ResourceNotFoundException;
import com.matador.shared.error.ValidationException;
import com.matador.shared.geo.GeoSupport;
import com.matador.shared.id.IdGenerator;
import com.matador.shared.storage.StoragePort;
import java.time.Clock;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Public API for the inspection module. */
@Service
public class InspectionService {

    private final InspectionRepository inspections;
    private final InspectionPhotoRepository photos;
    private final StoragePort storage;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    public InspectionService(
        InspectionRepository inspections,
        InspectionPhotoRepository photos,
        StoragePort storage,
        IdGenerator idGenerator,
        Clock clock,
        ApplicationEventPublisher events) {
        this.inspections = inspections;
        this.photos = photos;
        this.storage = storage;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.events = events;
    }

    public UploadUrlResponse presignUpload(UUID tripId, Phase phase, Angle angle, String contentType) {
        String key =
            "inspections/%s/%s/%s-%s.jpg".formatted(tripId, phase, angle, idGenerator.newId());
        StoragePort.PresignedUpload upload =
            storage.presignUpload(key, contentType == null ? "image/jpeg" : contentType);
        return new UploadUrlResponse(upload.uploadUrl(), upload.publicUrl(), upload.key());
    }

    @Transactional
    public InspectionResponse submit(
        UUID tripId, Phase phase, UUID submittedById, String role, SubmitInspectionRequest request) {
        Set<Angle> provided = EnumSet.noneOf(Angle.class);
        request.photos().forEach(p -> provided.add(p.angle()));
        if (!provided.containsAll(Angle.REQUIRED)) {
            throw new ValidationException(
                "MISSING_ANGLES", "All required inspection angles must be provided.");
        }

        Inspection inspection =
            new Inspection(
                idGenerator.newId(),
                tripId,
                phase,
                request.odometerMiles(),
                request.fuelChargePercent(),
                request.notes(),
                role,
                submittedById,
                clock.instant());
        inspections.save(inspection);

        for (PhotoSubmission p : request.photos()) {
            photos.save(
                new InspectionPhoto(
                    idGenerator.newId(),
                    inspection.getId(),
                    p.angle(),
                    p.url(),
                    p.capturedAt() == null ? clock.instant() : p.capturedAt(),
                    p.lat() != null && p.lng() != null ? GeoSupport.point(p.lng(), p.lat()) : null,
                    p.fileSizeBytes(),
                    p.widthPx(),
                    p.heightPx()));
        }

        events.publishEvent(
            new InspectionSubmitted(
                inspection.getId(), tripId, phase, request.odometerMiles(), inspection.getSubmittedAt()));
        return toResponse(inspection);
    }

    @Transactional(readOnly = true)
    public InspectionResponse get(UUID inspectionId) {
        return toResponse(require(inspectionId));
    }

    @Transactional
    public InspectionResponse review(UUID inspectionId, ReviewRequest request, UUID reviewerId) {
        Inspection inspection = require(inspectionId);
        inspection.review(request.status(), reviewerId, request.notes(), clock.instant());
        return toResponse(inspection);
    }

    @Transactional(readOnly = true)
    public boolean hasSubmittedInspection(UUID tripId, Phase phase) {
        return inspections.existsByTripIdAndPhase(tripId, phase);
    }

    private Inspection require(UUID inspectionId) {
        return inspections
            .findById(inspectionId)
            .orElseThrow(() -> ResourceNotFoundException.of("Inspection", inspectionId));
    }

    private InspectionResponse toResponse(Inspection i) {
        var photoViews =
            photos.findByInspectionId(i.getId()).stream()
                .map(p -> new PhotoView(p.getAngle(), p.getUrl()))
                .toList();
        return new InspectionResponse(
            i.getId(),
            i.getTripId(),
            i.getPhase(),
            i.getOdometerMiles(),
            i.getFuelChargePercent(),
            i.getReviewStatus(),
            photoViews);
    }
}
