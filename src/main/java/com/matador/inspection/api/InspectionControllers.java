package com.matador.inspection.api;

import com.matador.inspection.InspectionEnums.Phase;
import com.matador.inspection.InspectionService;
import com.matador.inspection.api.InspectionDtos.InspectionResponse;
import com.matador.inspection.api.InspectionDtos.ReviewRequest;
import com.matador.inspection.api.InspectionDtos.SubmitInspectionRequest;
import com.matador.inspection.api.InspectionDtos.UploadUrlRequest;
import com.matador.inspection.api.InspectionDtos.UploadUrlResponse;
import com.matador.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

public final class InspectionControllers {

    private InspectionControllers() {}

    @RestController
    @Tag(name = "Customer-Trips")
    public static class CustomerInspectionController {

        private final InspectionService inspectionService;

        public CustomerInspectionController(InspectionService inspectionService) {
            this.inspectionService = inspectionService;
        }

        @PostMapping("/api/customer/me/trips/{id}/inspections/{phase}/upload-url")
        @PreAuthorize("hasRole('CUSTOMER')")
        @Operation(summary = "Presign upload", description = "Get a presigned upload URL for a photo angle.")
        public UploadUrlResponse uploadUrl(
            @PathVariable UUID id, @PathVariable Phase phase, @Valid @RequestBody UploadUrlRequest request) {
            return inspectionService.presignUpload(id, phase, request.angle(), request.contentType());
        }

        @PostMapping("/api/customer/me/trips/{id}/inspections/{phase}")
        @ResponseStatus(HttpStatus.CREATED)
        @PreAuthorize("hasRole('CUSTOMER')")
        @Operation(summary = "Submit inspection", description = "Submit an inspection with all photo URLs.")
        public InspectionResponse submit(
            @PathVariable UUID id,
            @PathVariable Phase phase,
            @Valid @RequestBody SubmitInspectionRequest request) {
            return inspectionService.submit(id, phase, CurrentUser.requireId(), "CUSTOMER", request);
        }
    }

    @RestController
    @Tag(name = "Admin-Trips")
    public static class AdminInspectionController {

        private final InspectionService inspectionService;

        public AdminInspectionController(InspectionService inspectionService) {
            this.inspectionService = inspectionService;
        }

        @GetMapping("/api/admin/inspections/{id}")
        @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','SUPPORT','READONLY')")
        @Operation(summary = "Inspection detail", description = "View an inspection with photos.")
        public InspectionResponse get(@PathVariable UUID id) {
            return inspectionService.get(id);
        }

        @PostMapping("/api/admin/inspections/{id}/review")
        @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','SUPPORT')")
        @Operation(summary = "Review inspection", description = "Mark an inspection passed or flagged.")
        public InspectionResponse review(@PathVariable UUID id, @Valid @RequestBody ReviewRequest request) {
            return inspectionService.review(id, request, CurrentUser.requireId());
        }
    }
}
