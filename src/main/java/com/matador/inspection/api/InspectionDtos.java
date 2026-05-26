package com.matador.inspection.api;

import com.matador.inspection.InspectionEnums.Angle;
import com.matador.inspection.InspectionEnums.Phase;
import com.matador.inspection.InspectionEnums.ReviewStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class InspectionDtos {

    private InspectionDtos() {}

    public record UploadUrlRequest(@NotNull Angle angle, String contentType) {}

    public record UploadUrlResponse(String uploadUrl, String publicUrl, String key) {}

    public record PhotoSubmission(
        @NotNull Angle angle,
        @NotNull String url,
        Instant capturedAt,
        Double lat,
        Double lng,
        Integer fileSizeBytes,
        Integer widthPx,
        Integer heightPx) {}

    public record SubmitInspectionRequest(
        Integer odometerMiles,
        Integer fuelChargePercent,
        String notes,
        @NotEmpty List<PhotoSubmission> photos) {}

    public record InspectionResponse(
        UUID id,
        UUID tripId,
        Phase phase,
        Integer odometerMiles,
        Integer fuelChargePercent,
        ReviewStatus reviewStatus,
        List<PhotoView> photos) {}

    public record PhotoView(Angle angle, String url) {}

    public record ReviewRequest(@NotNull ReviewStatus status, String notes) {}
}
