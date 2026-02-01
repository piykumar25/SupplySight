/**
 * Bulk Actions Component
 * 
 * Action bar for selected alerts:
 * - Acknowledge selected
 * - Resolve selected
 * - Clear selection
 */

import { useState } from 'react';
import { CheckCircle2, XCircle, X, Loader2 } from 'lucide-react';
import { useBulkAcknowledgeMutation, useBulkResolveMutation } from '../queries';

/**
 * Props for BulkActions
 */
interface BulkActionsProps {
    selectedIds: Set<string>;
    onClearSelection: () => void;
    canAcknowledge: boolean;
    canResolve: boolean;
}

/**
 * Bulk Actions Component
 */
export function BulkActions({
    selectedIds,
    onClearSelection,
    canAcknowledge,
    canResolve,
}: BulkActionsProps) {
    const [showResolveComment, setShowResolveComment] = useState(false);
    const [resolveComment, setResolveComment] = useState('');

    const bulkAcknowledge = useBulkAcknowledgeMutation();
    const bulkResolve = useBulkResolveMutation();

    const count = selectedIds.size;
    const isProcessing = bulkAcknowledge.isPending || bulkResolve.isPending;

    const handleAcknowledge = async () => {
        try {
            await bulkAcknowledge.mutateAsync(Array.from(selectedIds));
            onClearSelection();
        } catch (error) {
            console.error('Failed to acknowledge alerts:', error);
        }
    };

    const handleResolve = async () => {
        try {
            await bulkResolve.mutateAsync({
                alertIds: Array.from(selectedIds),
                comment: resolveComment || undefined,
            });
            onClearSelection();
            setShowResolveComment(false);
            setResolveComment('');
        } catch (error) {
            console.error('Failed to resolve alerts:', error);
        }
    };

    if (count === 0) {
        return null;
    }

    return (
        <div className="flex items-center gap-4 p-3 rounded-lg bg-primary/10 border border-primary/30">
            {/* Selection count */}
            <div className="flex items-center gap-2">
                <span className="text-sm text-white font-medium">
                    {count} {count === 1 ? 'alert' : 'alerts'} selected
                </span>
                <button
                    onClick={onClearSelection}
                    className="p-1 rounded hover:bg-white/10 transition-colors text-gray-400 hover:text-white"
                    aria-label="Clear selection"
                >
                    <X className="w-4 h-4" />
                </button>
            </div>

            <div className="h-4 w-px bg-white/20" />

            {/* Actions */}
            <div className="flex items-center gap-2">
                {/* Acknowledge */}
                {canAcknowledge && (
                    <button
                        onClick={handleAcknowledge}
                        disabled={isProcessing}
                        className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-blue-500/20 text-blue-400 hover:bg-blue-500/30 disabled:opacity-50 transition-colors text-sm font-medium"
                    >
                        {bulkAcknowledge.isPending ? (
                            <Loader2 className="w-4 h-4 animate-spin" />
                        ) : (
                            <CheckCircle2 className="w-4 h-4" />
                        )}
                        <span>Acknowledge</span>
                    </button>
                )}

                {/* Resolve */}
                {canResolve && (
                    <>
                        {showResolveComment ? (
                            <div className="flex items-center gap-2">
                                <input
                                    type="text"
                                    value={resolveComment}
                                    onChange={(e) => setResolveComment(e.target.value)}
                                    placeholder="Resolution comment (optional)"
                                    className="px-3 py-1.5 rounded-lg border border-white/10 bg-white/5 text-sm text-white placeholder-gray-500 focus:border-green-500/50 focus:ring-0 focus:outline-none w-64"
                                    autoFocus
                                />
                                <button
                                    onClick={handleResolve}
                                    disabled={isProcessing}
                                    className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-green-500/20 text-green-400 hover:bg-green-500/30 disabled:opacity-50 transition-colors text-sm font-medium"
                                >
                                    {bulkResolve.isPending ? (
                                        <Loader2 className="w-4 h-4 animate-spin" />
                                    ) : (
                                        <span>Confirm</span>
                                    )}
                                </button>
                                <button
                                    onClick={() => {
                                        setShowResolveComment(false);
                                        setResolveComment('');
                                    }}
                                    className="p-1.5 rounded hover:bg-white/10 text-gray-400 hover:text-white transition-colors"
                                >
                                    <X className="w-4 h-4" />
                                </button>
                            </div>
                        ) : (
                            <button
                                onClick={() => setShowResolveComment(true)}
                                disabled={isProcessing}
                                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-green-500/20 text-green-400 hover:bg-green-500/30 disabled:opacity-50 transition-colors text-sm font-medium"
                            >
                                <XCircle className="w-4 h-4" />
                                <span>Resolve</span>
                            </button>
                        )}
                    </>
                )}
            </div>

            {/* Error display */}
            {(bulkAcknowledge.error || bulkResolve.error) && (
                <span className="text-xs text-red-400 ml-auto">
                    Failed to process. Please try again.
                </span>
            )}
        </div>
    );
}
