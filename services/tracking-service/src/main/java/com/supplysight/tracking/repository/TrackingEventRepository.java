package com.supplysight.tracking.repository;

import com.supplysight.tracking.domain.TrackingEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrackingEventRepository extends JpaRepository<TrackingEvent, UUID> {
    List<TrackingEvent> findByTenantIdAndTrackingNumberOrderByTimestampDesc(
            UUID tenantId, String trackingNumber);
}
