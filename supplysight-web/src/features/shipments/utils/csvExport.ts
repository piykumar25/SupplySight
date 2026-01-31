import { Shipment } from '@/types/shipment.types';
import { getRiskLevel, RiskLevel } from '../types';

/**
 * CSV column configuration
 */
interface CsvColumn {
    header: string;
    accessor: (shipment: Shipment) => string;
}

const CSV_COLUMNS: CsvColumn[] = [
    {
        header: 'Shipment ID',
        accessor: (s) => s.shipmentId,
    },
    {
        header: 'Tracking Number',
        accessor: (s) => s.trackingNumber,
    },
    {
        header: 'Status',
        accessor: (s) => s.status,
    },
    {
        header: 'Last Location',
        accessor: (s) => s.lastLocation?.hubCode || s.lastLocation?.address || 'N/A',
    },
    {
        header: 'Origin',
        accessor: (s) => s.origin?.hubCode || 'N/A',
    },
    {
        header: 'Destination',
        accessor: (s) => s.destination?.hubCode || 'N/A',
    },
    {
        header: 'ETA',
        accessor: (s) => s.eta || s.expectedDeliveryDate || 'N/A',
    },
    {
        header: 'Risk Level',
        accessor: (s) => {
            const level = getRiskLevel(s.delayProbability);
            const labels: Record<RiskLevel, string> = {
                all: 'All',
                normal: 'Low',
                delayed: 'Medium',
                high: 'High',
            };
            return labels[level];
        },
    },
    {
        header: 'Delay Probability',
        accessor: (s) => s.delayProbability !== undefined
            ? `${(s.delayProbability * 100).toFixed(1)}%`
            : 'N/A',
    },
    {
        header: 'Last Event Time',
        accessor: (s) => s.lastEventTime || s.updatedAt || 'N/A',
    },
    {
        header: 'Updated At',
        accessor: (s) => s.updatedAt,
    },
];

/**
 * Escape CSV value (handle commas, quotes, newlines)
 */
function escapeCsvValue(value: string): string {
    if (value.includes(',') || value.includes('"') || value.includes('\n')) {
        return `"${value.replace(/"/g, '""')}"`;
    }
    return value;
}

/**
 * Convert shipments to CSV string
 */
export function shipmentsToCSV(shipments: Shipment[]): string {
    const headers = CSV_COLUMNS.map((col) => col.header).join(',');

    const rows = shipments.map((shipment) =>
        CSV_COLUMNS.map((col) => escapeCsvValue(col.accessor(shipment))).join(',')
    );

    return [headers, ...rows].join('\n');
}

/**
 * Trigger CSV download
 */
export function downloadCSV(csvContent: string, filename: string = 'shipments-export.csv'): void {
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');

    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', filename);
    link.style.visibility = 'hidden';

    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    URL.revokeObjectURL(url);
}

/**
 * Export shipments to CSV file
 */
export function exportShipmentsToCSV(
    shipments: Shipment[],
    filename?: string
): void {
    const timestamp = new Date().toISOString().split('T')[0];
    const defaultFilename = `shipments-export-${timestamp}.csv`;

    const csvContent = shipmentsToCSV(shipments);
    downloadCSV(csvContent, filename || defaultFilename);
}
