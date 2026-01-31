import { MapPin, Clock, Server } from 'lucide-react';
import { ShipmentEvent } from '@/types/shipment.types';
import { getEventConfig, isExceptionalEvent } from '../types';

interface TimelineEventProps {
    event: ShipmentEvent;
    isFirst?: boolean;
    isLast?: boolean;
}

export function TimelineEvent({ event, isFirst, isLast }: TimelineEventProps) {
    const config = getEventConfig(event.eventType);
    const isExceptional = isExceptionalEvent(event.eventType);

    return (
        <div className="relative flex gap-4">
            {/* Timeline line */}
            <div className="flex flex-col items-center">
                {/* Top line */}
                <div className={`w-0.5 flex-1 ${isFirst ? 'bg-transparent' : 'bg-white/10'}`} />

                {/* Event dot */}
                <div
                    className={`
                        w-10 h-10 rounded-full flex items-center justify-center text-lg
                        ${isExceptional ? 'bg-red-500/20 ring-2 ring-red-500/50' : 'bg-white/10'}
                    `}
                >
                    {config.icon}
                </div>

                {/* Bottom line */}
                <div className={`w-0.5 flex-1 ${isLast ? 'bg-transparent' : 'bg-white/10'}`} />
            </div>

            {/* Event content */}
            <div className={`flex-1 pb-6 ${isLast ? 'pb-0' : ''}`}>
                <div
                    className={`
                        p-4 rounded-lg border transition-colors
                        ${isExceptional
                            ? 'bg-red-500/10 border-red-500/30 hover:bg-red-500/15'
                            : 'bg-white/5 border-white/10 hover:bg-white/10'
                        }
                    `}
                >
                    {/* Header row */}
                    <div className="flex items-start justify-between gap-2 mb-2">
                        <div className="flex items-center gap-2">
                            <span className={`font-medium ${config.color}`}>
                                {event.eventType.replace(/_/g, ' ')}
                            </span>
                            {isExceptional && (
                                <span className="px-2 py-0.5 text-xs font-medium bg-red-500/20 text-red-400 rounded">
                                    Attention
                                </span>
                            )}
                        </div>
                        <span className="text-gray-400 text-xs font-medium">
                            {event.source}
                        </span>
                    </div>

                    {/* Details row */}
                    <div className="flex flex-wrap items-center gap-4 text-sm text-gray-400">
                        {/* Timestamp */}
                        <div className="flex items-center gap-1">
                            <Clock className="w-3.5 h-3.5" />
                            <span>
                                {new Date(event.eventTime).toLocaleTimeString([], {
                                    hour: '2-digit',
                                    minute: '2-digit',
                                })}
                            </span>
                        </div>

                        {/* Location */}
                        {event.location && (
                            <div className="flex items-center gap-1">
                                <MapPin className="w-3.5 h-3.5" />
                                <span>
                                    {event.location.hubCode ||
                                        `${event.location.lat?.toFixed(2)}, ${event.location.lon?.toFixed(2)}`
                                    }
                                </span>
                            </div>
                        )}

                        {/* Source */}
                        <div className="flex items-center gap-1">
                            <Server className="w-3.5 h-3.5" />
                            <span>{event.source}</span>
                        </div>
                    </div>

                    {/* Payload/Remarks */}
                    {event.payload && Object.keys(event.payload).length > 0 && (
                        <div className="mt-3 pt-3 border-t border-white/10">
                            <div className="text-xs text-gray-500">
                                {Object.entries(event.payload)
                                    .filter(([_, v]) => v !== null && v !== undefined)
                                    .map(([key, value]) => (
                                        <span key={key} className="mr-3">
                                            <span className="text-gray-400">{key}:</span>{' '}
                                            <span className="text-gray-300">
                                                {typeof value === 'object' ? JSON.stringify(value) : String(value)}
                                            </span>
                                        </span>
                                    ))
                                }
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
