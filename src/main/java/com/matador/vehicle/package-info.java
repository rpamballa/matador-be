/**
 * Vehicle module: the fleet — physical cars, their classification, status, and location.
 * Exposes {@link com.matador.vehicle.VehicleService} and a {@link com.matador.vehicle.VehicleTelematicsPort}
 * that the telematics module implements for lock/unlock/status commands.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Vehicle")
package com.matador.vehicle;
