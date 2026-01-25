import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import { Shipment } from '@/types/shipment.types';
import L from 'leaflet';
import { useNavigate } from 'react-router-dom';

// Fix for default marker icon in Leaflet + Vite/Webpack
// In a real app, importing images properly or using custom SVGs is better
import icon from 'leaflet/dist/images/marker-icon.png';
import iconShadow from 'leaflet/dist/images/marker-shadow.png';

let DefaultIcon = L.icon({
    iconUrl: icon,
    shadowUrl: iconShadow,
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    popupAnchor: [1, -34],
});

L.Marker.prototype.options.icon = DefaultIcon;

interface MapWidgetProps {
    shipments: Shipment[];
    className?: string;
}

export function MapWidget({ shipments, className }: MapWidgetProps) {
    const navigate = useNavigate();

    // Default center (Bangalore/India approx) if no shipments
    const defaultCenter: [number, number] = [20.5937, 78.9629];
    const zoom = 4;

    // Filter shipments with valid location
    const validShipments = shipments.filter(s =>
        s.lastLocation?.lat && s.lastLocation?.lon
    );

    return (
        <div className={`rounded-xl overflow-hidden glass-panel border border-white/10 ${className}`}>
            <MapContainer
                center={defaultCenter}
                zoom={zoom}
                scrollWheelZoom={false}
                style={{ height: '100%', width: '100%', minHeight: '300px' }}
            >
                <TileLayer
                    attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />

                {validShipments.map((shipment) => (
                    <Marker
                        key={shipment.shipmentId}
                        position={[
                            shipment.lastLocation!.lat,
                            shipment.lastLocation!.lon
                        ] as [number, number]}
                    >
                        <Popup>
                            <div className="text-gray-900">
                                <strong>{shipment.trackingNumber}</strong><br />
                                Status: {shipment.status}<br />
                                <button
                                    className="text-primary hover:underline mt-1"
                                    onClick={() => navigate(`/shipments/${shipment.shipmentId}`)}
                                >
                                    View Details
                                </button>
                            </div>
                        </Popup>
                    </Marker>
                ))}
            </MapContainer>
        </div>
    );
}
