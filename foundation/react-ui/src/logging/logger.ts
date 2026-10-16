import { LOG_MASKING } from '../config.ts';
import { mask } from './masker.ts';
import type { MaskingMode } from './masker.ts';

export type Level = 'debug' | 'info' | 'warn' | 'error';
export type LogSink = Pick<Console, Level>;

export interface Logger {
  debug(message: string, ...details: unknown[]): void;
  info(message: string, ...details: unknown[]): void;
  warn(message: string, ...details: unknown[]): void;
  error(message: string, ...details: unknown[]): void;
}

function clean(value: unknown, mode: MaskingMode): unknown {
  if (typeof value === 'string') {
    return mask(value, mode);
  }
  if (value instanceof Error) {
    return mask(`${value.name}: ${value.message}`, mode);
  }
  if (value !== null && typeof value === 'object') {
    try {
      return mask(JSON.stringify(value), mode);
    } catch {
      return '[unserialisable]';
    }
  }
  return value;
}

/** A logger that redacts personal data from the message and from every detail before it reaches the sink (the console by default). */
export function createLogger(sink: LogSink = console, mode: MaskingMode = LOG_MASKING): Logger {
  const at =
    (level: Level) =>
    (message: string, ...details: unknown[]): void => {
      sink[level](mask(message, mode), ...details.map((detail) => clean(detail, mode)));
    };
  return { debug: at('debug'), info: at('info'), warn: at('warn'), error: at('error') };
}

export const logger: Logger = createLogger();
