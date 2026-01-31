import { useState, useEffect, useCallback, useMemo } from 'react';
import { MapContainer, TileLayer, Marker, Polyline, Popup, useMap } from 'react-leaflet';
import { Play, Pause, SkipForward, RotateCcw } from 'lucide-react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { ShipmentEvent, Location, ShipmentDetail } from '@/types/shipment.types';
import { sortEventsByTime, extractLocationsFromTimeline } from '../types';

// Fix for default marker icon
import icon from 'leaflet/dist/images/marker-icon.png';
import iconShadow from 'leaflet/dist/images/marker-shadow.png';

const DefaultIcon = L.icon({
    iconUrl: icon,
    shadowUrl: iconShadow,
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    popupAnchor: [1, -34],
});

L.Marker.prototype.options.icon = DefaultIcon;

// Current position marker (different color)
const CurrentIcon = L.icon({
    iconUrl: icon,
    shadowUrl: iconShadow,
    iconSize: [30, 48],
    iconAnchor: [15, 48],
    popupAnchor: [1, -38],
    className: 'current-marker-icon',
});

interface MapPanelProps {
    shipment: ShipmentDetail | undefined;
    events: ShipmentEvent[];
    isLoading: boolean;
}

// Component to fit bounds
function FitBounds({ locations }: { locations: Location[] }) {
    const map = useMap();

    useEffect(() => {
        if (locations.length > 0) {
            const bounds = L.latLngBounds(
                locations.map((loc) => [loc.lat, loc.lon] as [number, number])
            );
            map.fitBounds(bounds, { padding: [50, 50] });
        }
    }, [locations, map]);

    return null;
}

function Skeleton() {
    return (
        <div className="glass-panel p-6 animate-pulse">
            <div className="flex items-center gap-2 mb-4">
                <div className="w-5 h-5 bg-white/10 rounded" />
                <div className="h-5 w-32 bg-white/10 rounded" />
            </div>
            <div className="h-64 bg-white/10 rounded-lg" />
        </div>
    );
}

export function MapPanel({ shipment, events, isLoading }: MapPanelProps) {
    const [replayIndex, setReplayIndex] = useState(-1);
    const [isPlaying, setIsPlaying] = useState(false);

    const sortedEvents = useMemo(() => sortEventsByTime(events), [events]);

    const { locations, current } = useMemo(
        () => extractLocationsFromTimeline(
            sortedEvents,
            shipment?.origin,
            shipment?.destination
        ),
        [sortedEvents, shipment]
    );

    // Replay logic
    useEffect(() => {
        if (!isPlaying || replayIndex >= sortedEvents.length - 1) {
            setIsPlaying(false);
            return;
        }

        const timer = setTimeout(() => {
            setReplayIndex((prev) => prev + 1);
        }, 1000);

        return () => clearTimeout(timer);
    }, [isPlaying, replayIndex, sortedEvents.length]);

    const handlePlay = useCallback(() => {
        if (replayIndex >= sortedEvents.length - 1) {
            setReplayIndex(0);
        }
        setIsPlaying(true);
    }, [replayIndex, sortedEvents.length]);

    const handlePause = useCallback(() => {
        setIsPlaying(false);
    }, []);

    const handleStep = useCallback(() => {
        setIsPlaying(false);
        setReplayIndex((prev) => Math.min(prev + 1, sortedEvents.length - 1));
    }, [sortedEvents.length]);

    const handleReset = useCallback(() => {
        setIsPlaying(false);
        setReplayIndex(-1);
    }, []);

    // Get locations up to current replay index
    const visibleLocations = useMemo(() => {
        if (replayIndex < 0) return locations;

        const eventLocations: Location[] = [];
        if (shipment?.origin?.lat && shipment?.origin?.lon) {
            eventLocations.push(shipment.origin);
        }

        for (let i = 0; i <= replayIndex; i++) {
            const event = sortedEvents[i];
            if (event?.location?.lat && event?.location?.lon) {
                eventLocations.push(event.location);
            }
        }

        return eventLocations;
    }, [replayIndex, locations, sortedEvents, shipment?.origin]);

    const currentReplayLocation = useMemo(() => {
        if (replayIndex < 0) return current;
        const event = sortedEvents[replayIndex];
        return event?.location || current;
    }, [replayIndex, sortedEvents, current]);

    if (isLoading) {
        return <Skeleton />;
    }

    if (locations.length === 0) {
        return (
            <div className="glass-panel p-6">
                <h2 className="text-lg font-semibold text-white mb-4">Route Map</h2>
                <div className="flex flex-col items-center justify-center h-64 text-gray-500 bg-white/5 rounded-lg">
                    <p className="text-sm">No location data available</p>
                </div>
            </div>
        );
    }

    const defaultCenter: [number, number] = locations[0]
        ? [locations[0].lat, locations[0].lon]
        : [20.5937, 78.9629];

    return (
        <div className="glass-panel p-6">
            <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-semibold text-white">Route Map</h2>

                {/* Replay controls */}
                {sortedEvents.length > 1 && (
                    <div className="flex items-center gap-2">
                        <span className="text-xs text-gray-400 mr-2">
                            {replayIndex >= 0
                                ? `Event ${replayIndex + 1}/${sortedEvents.length}`
                                : 'Complete'}
                        </span>
                        <button
                            onClick={handleReset}
                            className="p-1.5 rounded hover:bg-white/10 transition-colors"
                            title="Reset"
                        >
                            <RotateCcw className="w-4 h-4 text-gray-400" />
                        </button>
                        {isPlaying ? (
                            <button
                                onClick={handlePause}
                                className="p-1.5 rounded hover:bg-white/10 transition-colors"
                                title="Pause"
                            >
                                <Pause className="w-4 h-4 text-gray-400" />
                            </button>
                        ) : (
                            <button
                                onClick={handlePlay}
                                className="p-1.5 rounded hover:bg-white/10 transition-colors"
                                title="Play replay"
                            >
                                <Play className="w-4 h-4 text-gray-400" />
                            </button>
                        )}
                        <button
                            onClick={handleStep}
                            className="p-1.5 rounded hover:bg-white/10 transition-colors"
                            title="Next event"
                        >
                            <SkipForward className="w-4 h-4 text-gray-400" />
                        </button>
                    </div>
                )}
            </div>

            <div className="rounded-lg overflow-hidden border border-white/10" style={{ height: '300px' }}>
                <MapContainer
                    center={defaultCenter}
                    zoom={5}
                    scrollWheelZoom={true}
                    style={{ height: '100%', width: '100%' }}
                >
                    <TileLayer
                        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                    />

                    <FitBounds locations={locations} />

                    {/* Polyline path */}
                    {visibleLocations.length > 1 && (
                        <Polyline
                            positions={visibleLocations.map((loc) => [loc.lat, loc.lon] as [number, number])}
                            color="#3b82f6"
                            weight={3}
                            opacity={0.8}
                        />
                    )}

                    {/* Origin marker */}
                    {shipment?.origin?.lat && shipment?.origin?.lon && (
                        <Marker position={[shipment.origin.lat, shipment.origin.lon]}>
                            <Popup>
                                <div className="text-gray-900">
                                    <strong>Origin</strong><br />
                                    {shipment.origin.hubCode || 'Starting point'}
                                </div>
                            </Popup>
                        </Marker>
                    )}

                    {/* Destination marker */}
                    {shipment?.destination?.lat && shipment?.destination?.lon && (
                        <Marker position={[shipment.destination.lat, shipment.destination.lon]}>
                            <Popup>
                                <div className="text-gray-900">
                                    <strong>Destination</strong><br />
                                    {shipment.destination.hubCode || 'Final destination'}
                                </div>
                            </Popup>
                        </Marker>
                    )}

                    {/* Current position marker */}
                    {currentReplayLocation?.lat && currentReplayLocation?.lon && (
                        <Marker
                            position={[currentReplayLocation.lat, currentReplayLocation.lon]}
                            icon={CurrentIcon}
                        >
                            <Popup>
                                <div className="text-gray-900">
                                    <strong>Current Location</strong><br />
                                    {currentReplayLocation.hubCode || 'Current position'}
                                </div>
                            </Popup>
                        </Marker>
                    )}
                </MapContainer>
            </div>
        </div>
    );
}
