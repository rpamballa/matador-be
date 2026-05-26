package com.matador.incident.internal;

import com.matador.incident.IncidentEnums.Status;
import com.matador.incident.IncidentEnums.Type;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    @Query(
        """
        select i from Incident i
        where (:status is null or i.status = :status)
          and (:type is null or i.type = :type)
          and (:tripId is null or i.tripId = :tripId)
        """)
    Page<Incident> search(
        @Param("status") Status status,
        @Param("type") Type type,
        @Param("tripId") UUID tripId,
        Pageable pageable);
}
