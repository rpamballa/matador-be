package com.matador.telematics.internal;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelematicsCommandLogRepository extends JpaRepository<TelematicsCommandLog, UUID> {}
