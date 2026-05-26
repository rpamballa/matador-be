package com.matador.zone.internal;

import com.matador.zone.Zone;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ZoneRepository extends JpaRepository<Zone, UUID> {

    List<Zone> findByActiveTrue();

    Optional<Zone> findBySlug(String slug);

    /** Returns the active zone whose boundary contains the point, if any (PostGIS). */
    @Query(
        value =
            """
            SELECT * FROM zone z
            WHERE z.active = TRUE
              AND ST_Contains(z.boundary::geometry,
                              ST_SetSRID(ST_MakePoint(:lng, :lat), 4326))
            LIMIT 1
            """,
        nativeQuery = true)
    Optional<Zone> findContaining(@Param("lat") double lat, @Param("lng") double lng);

    @Query(
        value =
            """
            SELECT ST_Contains(z.boundary::geometry,
                               ST_SetSRID(ST_MakePoint(:lng, :lat), 4326))
            FROM zone z WHERE z.id = :zoneId
            """,
        nativeQuery = true)
    Boolean contains(@Param("zoneId") UUID zoneId, @Param("lat") double lat, @Param("lng") double lng);
}
