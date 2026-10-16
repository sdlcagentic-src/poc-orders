// @vitest-environment node
import { describe, expect, it } from 'vitest';
import { LOG_MASKING, createLogger, mask } from '../src/index.ts';
import type { LogSink } from '../src/index.ts';

function sink(): { sink: LogSink; calls: Array<[string, unknown[]]> } {
  const calls: Array<[string, unknown[]]> = [];
  const record = (level: string) => (...args: unknown[]) => {
    calls.push([level, args]);
  };
  return { sink: { debug: record('debug'), info: record('info'), warn: record('warn'), error: record('error') }, calls };
}

describe('mask', () => {
  it('redacts email addresses in the email modes only', () => {
    expect(mask('mail jane.doe@example.com now', 'email')).toBe('mail [email] now');
    expect(mask('mail jane.doe@example.com now', 'email-phone')).toBe('mail [email] now');
    expect(mask('mail jane.doe@example.com now', 'none')).toBe('mail jane.doe@example.com now');
  });

  it('redacts phone numbers only when asked', () => {
    expect(mask('call +46 70 123 45 67 please', 'email-phone')).toBe('call [phone] please');
    expect(mask('call +46 70 123 45 67 please', 'email')).toBe('call +46 70 123 45 67 please');
  });

  it('redacts every occurrence', () => {
    expect(mask('a@b.io and c@d.io', 'email')).toBe('[email] and [email]');
  });
});

describe('createLogger', () => {
  it('masks the message and every detail before the sink sees it', () => {
    const { sink: s, calls } = sink();
    const log = createLogger(s, 'email');
    log.error('failed for jane@example.com', 'again jane@example.com', new Error('bad bob@example.org'), { user: 'eve@example.net' }, 42);
    expect(calls).toEqual([
      ['error', ['failed for [email]', 'again [email]', 'Error: bad [email]', '{"user":"[email]"}', 42]],
    ]);
  });

  it('uses the right level and survives details that cannot be serialised', () => {
    const { sink: s, calls } = sink();
    const log = createLogger(s, 'none');
    const loop: Record<string, unknown> = {};
    loop['self'] = loop;
    log.debug('d');
    log.info('i');
    log.warn('w', loop);
    expect(calls.map(([level]) => level)).toEqual(['debug', 'info', 'warn']);
    expect(calls[2]?.[1]).toEqual(['w', '[unserialisable]']);
  });

  it('has the masking mode of the profile by default', () => {
    expect(LOG_MASKING).toBe('email');
  });
});
