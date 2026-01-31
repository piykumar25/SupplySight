import { Brain, Clock, AlertTriangle, TrendingUp, Lightbulb } from 'lucide-react';
import { PredictionResult } from '@/types/shipment.types';
import { formatPredictionExplanation, RISK_CONFIG, getRiskLevel } from '../types';

interface PredictionPanelProps {
    prediction: PredictionResult | undefined;
    isLoading: boolean;
}

function Skeleton() {
    return (
        <div className="glass-panel p-6 animate-pulse">
            <div className="flex items-center gap-2 mb-4">
                <div className="w-5 h-5 bg-white/10 rounded" />
                <div className="h-5 w-32 bg-white/10 rounded" />
            </div>
            <div className="space-y-4">
                {[...Array(4)].map((_, i) => (
                    <div key={i} className="h-12 bg-white/10 rounded" />
                ))}
            </div>
        </div>
    );
}

export function PredictionPanel({ prediction, isLoading }: PredictionPanelProps) {
    if (isLoading) {
        return <Skeleton />;
    }

    if (!prediction) {
        return (
            <div className="glass-panel p-6">
                <h2 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
                    <Brain className="w-5 h-5 text-primary" />
                    Predictions
                </h2>
                <div className="flex flex-col items-center justify-center h-32 text-gray-500">
                    <p className="text-sm">No prediction data available</p>
                </div>
            </div>
        );
    }

    const riskLevel = getRiskLevel(prediction.delayProbability);
    const riskConfig = RISK_CONFIG[riskLevel];
    const explanations = formatPredictionExplanation(prediction);

    return (
        <div className="glass-panel p-6">
            <h2 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
                <Brain className="w-5 h-5 text-primary" />
                Predictions
            </h2>

            <div className="space-y-4">
                {/* ETA with confidence */}
                <div className="p-4 bg-white/5 rounded-lg">
                    <div className="flex items-center justify-between mb-2">
                        <span className="text-gray-400 text-sm flex items-center gap-1">
                            <Clock className="w-4 h-4" />
                            Estimated Arrival
                        </span>
                        <span className="text-xs text-gray-500">
                            {(prediction.etaConfidence * 100).toFixed(0)}% confidence
                        </span>
                    </div>
                    <div className="text-xl font-bold text-white">
                        {new Date(prediction.eta).toLocaleDateString('en-US', {
                            weekday: 'short',
                            month: 'short',
                            day: 'numeric',
                            hour: '2-digit',
                            minute: '2-digit',
                        })}
                    </div>
                    {/* Confidence bar */}
                    <div className="mt-2 w-full h-1.5 bg-white/10 rounded-full overflow-hidden">
                        <div
                            className="h-full bg-primary transition-all"
                            style={{ width: `${prediction.etaConfidence * 100}%` }}
                        />
                    </div>
                </div>

                {/* Delay Probability */}
                <div className="p-4 bg-white/5 rounded-lg">
                    <div className="flex items-center justify-between mb-2">
                        <span className="text-gray-400 text-sm flex items-center gap-1">
                            <AlertTriangle className="w-4 h-4" />
                            Delay Probability
                        </span>
                        <span className={`font-medium ${riskConfig.color}`}>
                            {prediction.delayRisk}
                        </span>
                    </div>
                    <div className="flex items-center gap-3">
                        <div className="flex-1 h-3 bg-white/10 rounded-full overflow-hidden">
                            <div
                                className={`h-full transition-all ${riskLevel === 'high' ? 'bg-red-500' :
                                        riskLevel === 'delayed' ? 'bg-yellow-500' :
                                            'bg-green-500'
                                    }`}
                                style={{ width: `${prediction.delayProbability * 100}%` }}
                            />
                        </div>
                        <span className={`text-lg font-bold ${riskConfig.color}`}>
                            {(prediction.delayProbability * 100).toFixed(0)}%
                        </span>
                    </div>
                </div>

                {/* Anomaly indicator */}
                {prediction.anomalyDetected && (
                    <div className="p-4 bg-red-500/10 border border-red-500/30 rounded-lg">
                        <div className="flex items-center gap-2 text-red-400 font-medium mb-2">
                            <AlertTriangle className="w-4 h-4" />
                            Anomaly Detected
                        </div>
                        <div className="flex flex-wrap gap-2">
                            {prediction.anomalyFlags.map((flag) => (
                                <span
                                    key={flag}
                                    className="px-2 py-1 text-xs bg-red-500/20 text-red-400 rounded"
                                >
                                    {flag.replace(/_/g, ' ')}
                                </span>
                            ))}
                        </div>
                    </div>
                )}

                {/* Factors breakdown */}
                <div className="p-4 bg-white/5 rounded-lg">
                    <div className="flex items-center gap-2 text-gray-400 text-sm mb-3">
                        <TrendingUp className="w-4 h-4" />
                        Prediction Factors
                    </div>
                    <div className="grid grid-cols-2 gap-3 text-sm">
                        <div>
                            <span className="text-gray-500">Distance</span>
                            <div className="text-white font-medium">
                                {prediction.factors.distanceRemaining.toFixed(0)} km
                            </div>
                        </div>
                        <div>
                            <span className="text-gray-500">Avg Speed</span>
                            <div className="text-white font-medium">
                                {prediction.factors.averageSpeed.toFixed(0)} km/h
                            </div>
                        </div>
                        <div>
                            <span className="text-gray-500">Historical On-Time</span>
                            <div className="text-white font-medium">
                                {(prediction.factors.historicalOnTime * 100).toFixed(0)}%
                            </div>
                        </div>
                        <div>
                            <span className="text-gray-500">Weather Impact</span>
                            <div className="text-white font-medium">
                                {(prediction.factors.weatherImpact * 100).toFixed(0)}%
                            </div>
                        </div>
                    </div>
                </div>

                {/* Why? section */}
                <div className="p-4 bg-white/5 rounded-lg">
                    <div className="flex items-center gap-2 text-gray-400 text-sm mb-3">
                        <Lightbulb className="w-4 h-4 text-yellow-400" />
                        Why?
                    </div>
                    <ul className="space-y-1">
                        {explanations.map((explanation, i) => (
                            <li key={i} className="text-sm text-gray-300 flex items-start gap-2">
                                <span className="text-gray-500">•</span>
                                {explanation}
                            </li>
                        ))}
                    </ul>
                </div>

                {/* Last updated */}
                <div className="text-xs text-gray-500 text-right">
                    Prediction updated: {new Date(prediction.updatedAt).toLocaleString()}
                </div>
            </div>
        </div>
    );
}
