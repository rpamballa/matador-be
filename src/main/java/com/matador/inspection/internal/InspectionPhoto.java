package com.matador.inspection.internal;

import com.matador.inspection.InspectionEnums.Angle;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "inspection_photo")
public class InspectionPhoto {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "inspection_id", nullable = false)
    private UUID inspectionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "angle", nullable = false)
    private Angle angle;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Column(name = "location", columnDefinition = "geography(Point,4326)")
    private Point location;

    @Column(name = "file_size_bytes")
    private Integer fileSizeBytes;

    @Column(name = "width_px")
    private Integer widthPx;

    @Column(name = "height_px")
    private Integer heightPx;

    protected InspectionPhoto() {}

    public InspectionPhoto(
        UUID id,
        UUID inspectionId,
        Angle angle,
        String url,
        Instant capturedAt,
        Point location,
        Integer fileSizeBytes,
        Integer widthPx,
        Integer heightPx) {
        this.id = id;
        this.inspectionId = inspectionId;
        this.angle = angle;
        this.url = url;
        this.capturedAt = capturedAt;
        this.location = location;
        this.fileSizeBytes = fileSizeBytes;
        this.widthPx = widthPx;
        this.heightPx = heightPx;
    }

    public Angle getAngle() {
        return angle;
    }

    public String getUrl() {
        return url;
    }
}
