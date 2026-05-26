package com.matador.incident.api;

import com.matador.incident.IncidentEnums.Status;
import com.matador.incident.IncidentEnums.Type;
import com.matador.incident.IncidentService;
import com.matador.incident.api.IncidentDtos.CustomerReportRequest;
import com.matador.incident.api.IncidentDtos.IncidentResponse;
import com.matador.incident.api.IncidentDtos.StaffCreateRequest;
import com.matador.incident.api.IncidentDtos.UpdateIncidentRequest;
import com.matador.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

public final class IncidentControllers {

    private IncidentControllers() {}

    @RestController
    @Tag(name = "Customer-Trips")
    public static class CustomerIncidentController {

        private final IncidentService incidentService;

        public CustomerIncidentController(IncidentService incidentService) {
            this.incidentService = incidentService;
        }

        @PostMapping("/api/customer/me/trips/{id}/incidents")
        @ResponseStatus(HttpStatus.CREATED)
        @PreAuthorize("hasRole('CUSTOMER')")
        @Operation(summary = "Report incident", description = "Customer reports an incident on a trip.")
        public IncidentResponse report(
            @PathVariable UUID id, @Valid @RequestBody CustomerReportRequest request) {
            return incidentService.reportByCustomer(CurrentUser.requireId(), id, request);
        }
    }

    @RestController
    @Tag(name = "Admin-Incidents")
    public static class AdminIncidentController {

        private final IncidentService incidentService;

        public AdminIncidentController(IncidentService incidentService) {
            this.incidentService = incidentService;
        }

        @PostMapping("/api/admin/incidents")
        @ResponseStatus(HttpStatus.CREATED)
        @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','SUPPORT')")
        @Operation(summary = "Create incident", description = "Staff creates an incident.")
        public IncidentResponse create(@Valid @RequestBody StaffCreateRequest request) {
            return incidentService.createByStaff(request);
        }

        @GetMapping("/api/admin/incidents")
        @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','SUPPORT','READONLY')")
        @Operation(summary = "List incidents", description = "Filterable incidents list.")
        public Page<IncidentResponse> list(
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) Type type,
            @RequestParam(required = false) UUID tripId,
            @PageableDefault(size = 20) Pageable pageable) {
            return incidentService.search(status, type, tripId, pageable);
        }

        @GetMapping("/api/admin/incidents/{id}")
        @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','SUPPORT','READONLY')")
        @Operation(summary = "Incident detail", description = "A single incident.")
        public IncidentResponse get(@PathVariable UUID id) {
            return incidentService.get(id);
        }

        @PatchMapping("/api/admin/incidents/{id}")
        @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','SUPPORT')")
        @Operation(summary = "Update incident", description = "Update status, resolution, and charges.")
        public IncidentResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateIncidentRequest request) {
            return incidentService.update(id, request);
        }
    }
}
