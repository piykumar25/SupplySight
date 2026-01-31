import { useState } from 'react';
import { Copy, ExternalLink, Check } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

interface RowActionsProps {
    shipmentId: string;
}

export function RowActions({ shipmentId }: RowActionsProps) {
    const navigate = useNavigate();
    const [copied, setCopied] = useState(false);

    const handleCopyId = (e: React.MouseEvent) => {
        e.stopPropagation();
        navigator.clipboard.writeText(shipmentId);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    };

    const handleViewDetails = (e: React.MouseEvent) => {
        e.stopPropagation();
        navigate(`/shipments/${shipmentId}`);
    };

    return (
        <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
            <button
                onClick={handleCopyId}
                className="p-1.5 rounded hover:bg-white/10 transition-colors"
                title="Copy ID"
            >
                {copied ? (
                    <Check className="w-4 h-4 text-green-400" />
                ) : (
                    <Copy className="w-4 h-4 text-gray-400" />
                )}
            </button>
            <button
                onClick={handleViewDetails}
                className="p-1.5 rounded hover:bg-white/10 transition-colors"
                title="View Details"
            >
                <ExternalLink className="w-4 h-4 text-gray-400" />
            </button>
        </div>
    );
}
