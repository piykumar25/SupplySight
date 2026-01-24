import { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Package, MapPin, Calendar, Clock, ChevronRight, Search, Zap } from 'lucide-react';

// Mock Data for Demo
const MOCK_SHIPMENTS = [
    {
        id: 'SH-102938',
        trackingNumber: 'TRK-9928-XA',
        origin: 'Shanghai, CN',
        destination: 'Los Angeles, USA',
        status: 'IN_TRANSIT',
        eta: '2026-02-12T14:00:00',
        events: [
            { id: '1', status: 'IN_TRANSIT', location: 'Pacific Ocean', timestamp: '2026-01-24T08:00:00', description: 'Vessel departure confirmed' },
            { id: '2', status: 'PICKED_UP', location: 'Shanghai Port', timestamp: '2026-01-23T10:30:00', description: 'Cargo loaded onto vessel' },
            { id: '3', status: 'CREATED', location: 'Shanghai Warehouse', timestamp: '2026-01-22T09:15:00', description: 'Shipment created' },
        ]
    },
    {
        id: 'SH-554422',
        trackingNumber: 'TRK-1122-BB',
        origin: 'Berlin, DE',
        destination: 'Paris, FR',
        status: 'DELIVERED',
        eta: '2026-01-20T11:00:00',
        events: [
            { id: '4', status: 'DELIVERED', location: 'Paris, FR', timestamp: '2026-01-20T11:45:00', description: 'Delivered to recipient' },
            { id: '5', status: 'OUT_FOR_DELIVERY', location: 'Paris Hub', timestamp: '2026-01-20T08:00:00', description: 'Out for delivery' },
        ]
    },
    {
        id: 'SH-998877',
        trackingNumber: 'TRK-7766-CC',
        origin: 'New York, USA',
        destination: 'Chicago, USA',
        status: 'DELAYED',
        eta: '2026-01-25T16:00:00',
        events: [
            { id: '6', status: 'DELAYED', location: 'Cleveland, OH', timestamp: '2026-01-24T12:00:00', description: 'Severe weather delay' },
            { id: '7', status: 'IN_TRANSIT', location: 'Pittsburgh, PA', timestamp: '2026-01-24T06:00:00', description: 'Departed facility' },
        ]
    }
];

export default function Dashboard() {
    const [shipments, setShipments] = useState(MOCK_SHIPMENTS);
    const [selectedId, setSelectedId] = useState(MOCK_SHIPMENTS[0].id);
    const [loading, setLoading] = useState(false);

    const selectedShipment = shipments.find(s => s.id === selectedId);

    // Simulated Fetch with Fallback
    useEffect(() => {
        // In real app, fetch from API. Here we use mock.
        // fetch('http://localhost:8083/api/v1/shipments')...
    }, []);

    const getStatusColor = (status) => {
        switch (status) {
            case 'DELIVERED': return 'text-green-400 bg-green-500/10 border-green-500/20';
            case 'DELAYED': return 'text-red-400 bg-red-500/10 border-red-500/20';
            case 'IN_TRANSIT': return 'text-blue-400 bg-blue-500/10 border-blue-500/20';
            default: return 'text-gray-400 bg-gray-500/10 border-gray-500/20';
        }
    };

    return (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 h-[calc(100vh-100px)]">

            {/* Shipment List */}
            <div className="glass-panel p-4 flex flex-col h-full overflow-hidden">
                <div className="flex items-center gap-3 mb-6">
                    <div className="relative flex-1">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" />
                        <input
                            type="text"
                            placeholder="Search tracking..."
                            className="input-field pl-9 text-sm py-1.5"
                        />
                    </div>
                    <button className="p-2 bg-primary/20 text-primary rounded-lg hover:bg-primary/30 transition-colors">
                        <Zap className="w-4 h-4" />
                    </button>
                </div>

                <div className="space-y-3 overflow-y-auto pr-2 custom-scrollbar flex-1">
                    {shipments.map(shipment => (
                        <div
                            key={shipment.id}
                            onClick={() => setSelectedId(shipment.id)}
                            className={`p-4 rounded-xl border transition-all cursor-pointer group ${selectedId === shipment.id
                                    ? 'bg-white/10 border-primary/50 shadow-lg shadow-primary/5'
                                    : 'bg-white/5 border-transparent hover:bg-white/10'
                                }`}
                        >
                            <div className="flex justify-between items-start mb-2">
                                <span className="font-mono text-sm text-gray-400">{shipment.trackingNumber}</span>
                                <span className={`text-[10px] px-2 py-0.5 rounded-full border ${getStatusColor(shipment.status)}`}>
                                    {shipment.status.replace('_', ' ')}
                                </span>
                            </div>
                            <div className="flex items-center gap-2 text-sm font-medium mb-1">
                                <span className="text-white">{shipment.origin.split(',')[0]}</span>
                                <ChevronRight className="w-4 h-4 text-gray-600" />
                                <span className="text-white">{shipment.destination.split(',')[0]}</span>
                            </div>
                            <div className="flex items-center gap-1 text-xs text-gray-500 mt-2">
                                <Clock className="w-3 h-3" />
                                <span>ETA: {new Date(shipment.eta).toLocaleDateString()}</span>
                            </div>
                        </div>
                    ))}
                </div>
            </div>

            {/* Main View: Map & Details */}
            <div className="lg:col-span-2 flex flex-col gap-6 h-full overflow-hidden">

                {/* Detail Header */}
                <div className="glass-panel p-6 flex flex-wrap gap-4 justify-between items-center shrink-0">
                    <div>
                        <h2 className="text-2xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-white to-gray-400">
                            {selectedShipment.trackingNumber}
                        </h2>
                        <div className="flex items-center gap-2 text-gray-400 mt-1">
                            <MapPin className="w-4 h-4 text-primary" />
                            <span>Current Location: {selectedShipment.events[0]?.location || 'Unknown'}</span>
                        </div>
                    </div>
                    <div className="text-right">
                        <div className="text-sm text-gray-500">Estimated Delivery</div>
                        <div className="text-xl font-mono text-primary font-bold">
                            {new Date(selectedShipment.eta).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}
                        </div>
                    </div>
                </div>

                {/* Map Visualization (Mock) */}
                <div className="glass-panel relative flex-1 min-h-[300px] overflow-hidden group">
                    <div className="absolute inset-0 bg-[#1a1d24] flex items-center justify-center">
                        {/* Abstract Map Grid */}
                        <div className="absolute inset-0 opacity-20"
                            style={{ backgroundImage: 'radial-gradient(#4b5563 1px, transparent 1px)', backgroundSize: '20px 20px' }}
                        />
                        {/* Connection Line */}
                        <div className="absolute top-1/2 left-[20%] w-[60%] h-[2px] bg-gradient-to-r from-primary/20 via-primary to-primary/20" />

                        {/* Origin Node */}
                        <div className="absolute top-1/2 left-[20%] -translate-y-1/2 -translate-x-1/2 flex flex-col items-center">
                            <div className="w-4 h-4 bg-gray-500 rounded-full border-4 border-[#1a1d24]" />
                            <span className="mt-2 text-xs font-mono text-gray-500">{selectedShipment.origin}</span>
                        </div>

                        {/* Destination Node */}
                        <div className="absolute top-1/2 left-[80%] -translate-y-1/2 -translate-x-1/2 flex flex-col items-center">
                            <div className="w-4 h-4 bg-gray-500 rounded-full border-4 border-[#1a1d24]" />
                            <span className="mt-2 text-xs font-mono text-gray-500">{selectedShipment.destination}</span>
                        </div>

                        {/* Moving Package (Animation) */}
                        <div className="absolute top-1/2 left-[50%] -translate-y-1/2 -translate-x-1/2 z-10 animate-pulse">
                            <div className="relative">
                                <div className="absolute inset-0 bg-primary blur-lg opacity-50" />
                                <Package className="w-8 h-8 text-white relative z-10 drop-shadow-[0_0_10px_rgba(59,130,246,0.5)]" />
                            </div>
                            <div className="absolute top-8 left-1/2 -translate-x-1/2 whitespace-nowrap bg-black/50 backdrop-blur px-2 py-0.5 rounded text-[10px] border border-white/10">
                                In Transit &bull; 45mph
                            </div>
                        </div>
                    </div>
                </div>

                {/* Timeline */}
                <div className="glass-panel p-6 max-h-[250px] overflow-y-auto shrink-0 custom-scrollbar">
                    <h3 className="text-sm font-bold text-gray-400 uppercase tracking-wider mb-4">Shipment History</h3>
                    <div className="space-y-6 relative ml-2">
                        <div className="absolute left-[7px] top-2 bottom-2 w-[2px] bg-white/5" />
                        {selectedShipment.events.map((event, i) => (
                            <div key={event.id} className="relative pl-8 animate-in slide-in-from-bottom-2 duration-500" style={{ animationDelay: `${i * 100}ms` }}>
                                <div className={`absolute left-0 top-1.5 w-4 h-4 rounded-full border-2 ${i === 0 ? 'bg-primary border-primary shadow-[0_0_10px_rgba(59,130,246,0.5)]' : 'bg-[#1a1d24] border-gray-600'
                                    }`} />
                                <div className="flex justify-between items-start">
                                    <div>
                                        <p className={`font-medium ${i === 0 ? 'text-white' : 'text-gray-400'}`}>{event.description}</p>
                                        <p className="text-sm text-gray-500 mt-0.5">{event.location}</p>
                                    </div>
                                    <div className="text-right">
                                        <p className="text-xs font-mono text-gray-500">
                                            {new Date(event.timestamp).toLocaleDateString()}
                                        </p>
                                        <p className="text-xs font-mono text-gray-600">
                                            {new Date(event.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                        </p>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
}
