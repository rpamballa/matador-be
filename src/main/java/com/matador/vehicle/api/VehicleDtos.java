package com.matador.vehicle.api;

import com.matador.vehicle.Drivetrain;
import com.matador.vehicle.VehicleStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public final class VehicleDtos {

    private VehicleDtos() {}

    // ---- Vehicle class ----

    public record CreateVehicleClassRequest(
        @NotBlank String name,
        String description,
        @Min(1) int seats,
        @Min(0) int luggageCapacity,
        @NotNull Drivetrain drivetrain,
        @Min(0) long baseDailyRateCents,
        int sortOrder) {}

    public record UpdateVehicleClassRequest(
        String description, Long baseDailyRateCents, Integer sortOrder, Boolean active) {}

    public record VehicleClassResponse(
        UUID id,
        String name,
        String description,
        int seats,
        int luggageCapacity,
        Drivetrain drivetrain,
        long baseDailyRateCents,
        int sortOrder,
        boolean active) {}

    // ---- Vehicle ----

    public record CreateVehicleRequest(
        @NotBlank String vin,
        @NotBlank String licensePlate,
        @NotBlank @Size(min = 2, max = 2) String licenseState,
        @NotBlank String make,
        @NotBlank String model,
        @Min(1900) int year,
        @NotBlank String color,
        @NotNull UUID classId,
        @NotNull UUID homeZoneId,
        @NotNull LocalDate acquiredOn,
        String telematicsProvider,
        String telematicsVehicleId) {}

    public record UpdateVehicleRequest(String color, String licensePlate, String notes) {}

    public record TransitionStatusRequest(@NotNull VehicleStatus status) {}

    public record VehicleResponse(
        UUID id,
        String vin,
        String licensePlate,
        String licenseState,
        String make,
        String model,
        int year,
        String color,
        UUID classId,
        VehicleStatus status,
        Double lat,
        Double lng,
        String currentAddress,
        int odometerMiles,
        Integer fuelChargePercent,
        Integer rangeMiles,
        UUID homeZoneId,
        String telematicsProvider,
        LocalDate acquiredOn,
        LocalDate retiredOn) {}

    public record CommandResponse(boolean succeeded, String detail) {}

    // ---- Customer-facing availability ----

    public record AvailableClassResponse(
        UUID classId,
        String name,
        String description,
        Drivetrain drivetrain,
        int seats,
        int luggageCapacity,
        long countAvailable,
        long startingDailyRateCents,
        String representativePhotoUrl) {}
}
