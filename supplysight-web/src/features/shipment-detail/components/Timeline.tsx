import { History } from 'lucide-react';
import { ShipmentEvent } from '@/types/shipment.types';
import { groupEventsByDate } from '../types';
import { TimelineEvent } from './TimelineEvent';

interface TimelineProps {
    events: ShipmentEvent[];
    isLoading: boolean;
}

function Skeleton() {
    return (
        <div className="glass-panel p-6 animate-pulse">
            <div className="flex items-center gap-2 mb-6">
                <div className="w-5 h-5 bg-white/10 rounded" />
                <div className="h-5 w-32 bg-white/10 rounded" />
            </div>
            <div className="space-y-4">
                {[...Array(4)].map((_, i) => (
                    <div key={i} className="flex gap-4">
                        <div className="w-10 h-10 bg-white/10 rounded-full" />
                        <div className="flex-1 h-24 bg-white/10 rounded-lg" />
                    </div>
                ))}
            </div>
        </div>
    );
}

export function Timeline({ events, isLoading }: TimelineProps) {
    if (isLoading) {
        return <Skeleton />;
    }

    if (!events || events.length === 0) {
        return (
            <div className="glass-panel p-6">
                <h2 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
                    <History className="w-5 h-5 text-primary" />
                    Event Timeline
                </h2>
                <div className="flex flex-col items-center justify-center h-32 text-gray-500">
                    <p className="text-sm">No events recorded yet</p>
                </div>
            </div>
        );
    }

    // Group events by date (sorted chronologically)
    const groups = groupEventsByDate(events);

    return (
        <div className="glass-panel p-6">
            <div className="flex items-center justify-between mb-6">
                <h2 className="text-lg font-semibold text-white flex items-center gap-2">
                    <History className="w-5 h-5 text-primary" />
                    Event Timeline
                </h2>
                <span className="text-sm text-gray-400">
                    {events.length} event{events.length !== 1 ? 's' : ''}
                </span>
            </div>

            <div className="space-y-6">
                {groups.map((group, groupIndex) => (
                    <div key={group.date}>
                        {/* Date header */}
                        <div className="sticky top-0 z-10 bg-surface/90 backdrop-blur-sm py-2 mb-3 -mx-6 px-6 border-b border-white/5">
                            <span className="text-sm font-medium text-gray-300">
                                {group.dateLabel}
                            </span>
                            <span className="text-xs text-gray-500 ml-2">
                                {group.date}
                            </span>
                        </div>

                        {/* Events in this group */}
                        <div>
                            {group.events.map((event, eventIndex) => (
                                <TimelineEvent
                                    key={event.eventId}
                                    event={event}
                                    isFirst={groupIndex === 0 && eventIndex === 0}
                                    isLast={
                                        groupIndex === groups.length - 1 &&
                                        eventIndex === group.events.length - 1
                                    }
                                />
                            ))}
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}
