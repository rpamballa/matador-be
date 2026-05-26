package com.matador.trip.internal;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripLocationSampleRepository extends JpaRepository<TripLocationSample, UUID> {}
