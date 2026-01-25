import { predictionApi } from './axios';
import { ApiResult } from '../types/api.types';
import { PredictionResult } from '../types/shipment.types';

export const predictionClient = {
    /**
     * Get prediction for a shipment
     */
    getPrediction: async (shipmentId: string) => {
        const response = await predictionApi.get<ApiResult<PredictionResult>>(
            `/predictions/${shipmentId}`
        );
        return response.data;
    }
};
