package com.supplysight.tracking.repository;

import com.supplysight.tracking.domain.TrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrackingEventRepository extends JpaRepository<TrackingEvent, String> {
    List<TrackingEvent> findByTrackingNumberOrderByTimestampDesc(String trackingNumber);
}
