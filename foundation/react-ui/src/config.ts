import type { MaskingMode } from './logging/masker.ts';

/** How long a request may take before it is abandoned, in milliseconds (profile option request-timeout-ms). */
export const REQUEST_TIMEOUT_MS = 5000;

/** Which personal data the logger redacts (profile option log-masking). */
export const LOG_MASKING: MaskingMode = 'email';
