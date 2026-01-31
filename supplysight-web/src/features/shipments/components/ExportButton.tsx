import { useState } from 'react';
import { Download, Loader2 } from 'lucide-react';
import { Shipment } from '@/types/shipment.types';
import { exportShipmentsToCSV } from '../utils/csvExport';

interface ExportButtonProps {
    shipments: Shipment[];
    disabled?: boolean;
}

export function ExportButton({ shipments, disabled }: ExportButtonProps) {
    const [isExporting, setIsExporting] = useState(false);

    const handleExport = async () => {
        if (shipments.length === 0) return;

        setIsExporting(true);

        // Small delay to show loading state
        await new Promise((resolve) => setTimeout(resolve, 300));

        try {
            exportShipmentsToCSV(shipments);
        } catch (error) {
            console.error('Export failed:', error);
        } finally {
            setIsExporting(false);
        }
    };

    return (
        <button
            onClick={handleExport}
            disabled={disabled || isExporting || shipments.length === 0}
            className="btn-secondary flex items-center gap-2"
            title={shipments.length === 0 ? 'No data to export' : `Export ${shipments.length} shipments`}
        >
            {isExporting ? (
                <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
                <Download className="w-4 h-4" />
            )}
            <span>Export</span>
        </button>
    );
}
