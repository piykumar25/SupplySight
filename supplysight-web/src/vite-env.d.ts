/// <reference types="vite/client" />

interface ImportMetaEnv {
    readonly VITE_IDENTITY_API_URL: string;
    readonly VITE_TRACKING_API_URL: string;
    readonly VITE_VISIBILITY_API_URL: string;
    readonly VITE_PREDICTION_API_URL: string;
}

interface ImportMeta {
    readonly env: ImportMetaEnv;
}
