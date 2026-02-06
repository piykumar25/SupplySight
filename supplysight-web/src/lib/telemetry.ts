/**
 * Frontend Telemetry Module
 * Tracks page load timing, API latency, SSE disconnects, and errors.
 * Optionally integrates with Sentry when DSN is configured.
 */

interface TelemetryEvent {
  name: string;
  properties: Record<string, string | number | boolean>;
  timestamp: number;
}

interface TelemetryConfig {
  enabled: boolean;
  sentryDsn?: string;
  metricsEndpoint?: string;
  sampleRate: number;
}

class Telemetry {
  private static instance: Telemetry;
  private config: TelemetryConfig = {
    enabled: true,
    sampleRate: 1.0,
  };
  private events: TelemetryEvent[] = [];
  private sseDisconnects = 0;
  private sseConnections = 0;
  private errorCount = 0;
  private apiLatencies: Map<string, number[]> = new Map();

  private constructor() {
    this.initPerformanceObserver();
    this.initErrorTracking();
  }

  static getInstance(): Telemetry {
    if (!Telemetry.instance) {
      Telemetry.instance = new Telemetry();
    }
    return Telemetry.instance;
  }

  configure(config: Partial<TelemetryConfig>): void {
    this.config = { ...this.config, ...config };
  }

  // Performance Metrics
  private initPerformanceObserver(): void {
    if (typeof window === 'undefined' || !window.PerformanceObserver) return;

    // Track page load timing
    const pageLoadObserver = new PerformanceObserver((list) => {
      for (const entry of list.getEntries()) {
        if (entry.entryType === 'navigation') {
          const nav = entry as PerformanceNavigationTiming;
          this.track('page_load', {
            dns: nav.domainLookupEnd - nav.domainLookupStart,
            tcp: nav.connectEnd - nav.connectStart,
            ttfb: nav.responseStart - nav.requestStart,
            dom: nav.domContentLoadedEventEnd - nav.responseEnd,
            load: nav.loadEventEnd - nav.loadEventStart,
            total: nav.loadEventEnd - nav.startTime,
          });
        }
      }
    });

    try {
      pageLoadObserver.observe({ entryTypes: ['navigation'] });
    } catch {
      // Fallback for browsers without PerformanceObserver support
    }

    // Track largest contentful paint
    const lcpObserver = new PerformanceObserver((list) => {
      const entries = list.getEntries();
      if (entries.length > 0) {
        const lastEntry = entries[entries.length - 1];
        this.track('lcp', { value: lastEntry.startTime });
      }
    });

    try {
      lcpObserver.observe({ entryTypes: ['largest-contentful-paint'] });
    } catch {
      // Not supported
    }
  }

  // Error Tracking
  private initErrorTracking(): void {
    if (typeof window === 'undefined') return;

    window.addEventListener('error', (event) => {
      this.trackError('uncaught_error', event.error || event.message, {
        filename: event.filename,
        lineno: event.lineno,
        colno: event.colno,
      });
    });

    window.addEventListener('unhandledrejection', (event) => {
      this.trackError('unhandled_rejection', event.reason);
    });
  }

  // Track custom events
  track(name: string, properties: Record<string, string | number | boolean>): void {
    if (!this.config.enabled) return;
    if (Math.random() > this.config.sampleRate) return;

    const event: TelemetryEvent = {
      name,
      properties: {
        ...properties,
        tenantId: this.getTenantId(),
        userId: this.getUserId(),
        url: typeof window !== 'undefined' ? window.location.pathname : '',
      },
      timestamp: Date.now(),
    };

    this.events.push(event);
    
    // Keep only last 100 events in memory
    if (this.events.length > 100) {
      this.events.shift();
    }

    // Log in development
    if (import.meta.env.DEV) {
      console.debug('[Telemetry]', name, properties);
    }
  }

  // Track API latency
  trackApiLatency(endpoint: string, method: string, latencyMs: number, status: number): void {
    const key = `${method}:${this.normalizeEndpoint(endpoint)}`;
    
    if (!this.apiLatencies.has(key)) {
      this.apiLatencies.set(key, []);
    }
    
    const latencies = this.apiLatencies.get(key)!;
    latencies.push(latencyMs);
    
    // Keep only last 50 samples per endpoint
    if (latencies.length > 50) {
      latencies.shift();
    }

    this.track('api_request', {
      endpoint: this.normalizeEndpoint(endpoint),
      method,
      latencyMs,
      status,
      isError: status >= 400,
    });
  }

  // Track SSE events
  trackSSEConnect(): void {
    this.sseConnections++;
    this.track('sse_connect', { total: this.sseConnections });
  }

  trackSSEDisconnect(reason?: string): void {
    this.sseDisconnects++;
    this.track('sse_disconnect', { 
      total: this.sseDisconnects, 
      reason: reason || 'unknown' 
    });
  }

  // Track errors
  trackError(
    type: string, 
    error: Error | string, 
    context?: Record<string, string | number>
  ): void {
    this.errorCount++;
    
    const errorMessage = error instanceof Error ? error.message : String(error);
    const errorStack = error instanceof Error ? error.stack : undefined;

    this.track('error', {
      type,
      message: this.redactPII(errorMessage),
      stack: errorStack ? this.redactPII(errorStack.substring(0, 500)) : '',
      count: this.errorCount,
      ...context,
    });
  }

  // Get metrics summary
  getMetrics(): Record<string, unknown> {
    const apiLatencySummary: Record<string, { avg: number; p95: number; count: number }> = {};
    
    this.apiLatencies.forEach((latencies, key) => {
      if (latencies.length > 0) {
        const sorted = [...latencies].sort((a, b) => a - b);
        const p95Index = Math.floor(sorted.length * 0.95);
        apiLatencySummary[key] = {
          avg: latencies.reduce((a, b) => a + b, 0) / latencies.length,
          p95: sorted[p95Index] || sorted[sorted.length - 1],
          count: latencies.length,
        };
      }
    });

    return {
      sseConnections: this.sseConnections,
      sseDisconnects: this.sseDisconnects,
      errorCount: this.errorCount,
      apiLatency: apiLatencySummary,
      eventCount: this.events.length,
    };
  }

  // Clear metrics (useful for testing)
  reset(): void {
    this.events = [];
    this.sseDisconnects = 0;
    this.sseConnections = 0;
    this.errorCount = 0;
    this.apiLatencies.clear();
  }

  // Helpers
  private getTenantId(): string {
    // Get from auth store or localStorage
    try {
      const token = localStorage.getItem('accessToken');
      if (token) {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return payload.tenantId || 'unknown';
      }
    } catch {
      // Invalid token
    }
    return 'unknown';
  }

  private getUserId(): string {
    try {
      const token = localStorage.getItem('accessToken');
      if (token) {
        const payload = JSON.parse(atob(token.split('.')[1]));
        // Redact to first 8 chars for privacy
        const userId = payload.sub || payload.userId;
        return userId ? userId.substring(0, 8) + '...' : 'unknown';
      }
    } catch {
      // Invalid token
    }
    return 'unknown';
  }

  private normalizeEndpoint(endpoint: string): string {
    // Replace UUIDs with {id}
    return endpoint.replace(
      /[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}/gi,
      '{id}'
    );
  }

  private redactPII(text: string): string {
    // Redact email addresses
    text = text.replace(/[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}/g, '[EMAIL]');
    // Redact JWT tokens
    text = text.replace(/eyJ[a-zA-Z0-9_-]*\.eyJ[a-zA-Z0-9_-]*\.[a-zA-Z0-9_-]*/g, '[JWT]');
    // Redact potential API keys
    text = text.replace(/[a-zA-Z0-9]{32,}/g, '[KEY]');
    return text;
  }
}

// Export singleton instance
export const telemetry = Telemetry.getInstance();

// Axios interceptor for API latency tracking
export const createTelemetryInterceptor = () => {
  return {
    request: (config: { url?: string; method?: string; metadata?: { startTime: number } }) => {
      config.metadata = { startTime: Date.now() };
      return config;
    },
    response: (response: { config: { url: string; method: string; metadata?: { startTime: number } }; status: number }) => {
      const latency = response.config.metadata?.startTime 
        ? Date.now() - response.config.metadata.startTime 
        : 0;
      telemetry.trackApiLatency(
        response.config.url,
        response.config.method?.toUpperCase() || 'GET',
        latency,
        response.status
      );
      return response;
    },
    error: (error: { config?: { url: string; method: string; metadata?: { startTime: number } }; response?: { status: number } }) => {
      if (error.config) {
        const latency = error.config.metadata?.startTime 
          ? Date.now() - error.config.metadata.startTime 
          : 0;
        telemetry.trackApiLatency(
          error.config.url,
          error.config.method?.toUpperCase() || 'GET',
          latency,
          error.response?.status || 0
        );
      }
      throw error;
    },
  };
};
