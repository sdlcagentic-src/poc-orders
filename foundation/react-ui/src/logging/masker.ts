export type MaskingMode = 'none' | 'email' | 'email-phone';

const EMAIL = /[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}/g;
const PHONE = /\+?[0-9][0-9 ()-]{7,}[0-9]/g;

/** Redacts personal data from log text: nothing, email addresses, or email addresses and phone numbers. */
export function mask(text: string, mode: MaskingMode): string {
  const withoutEmail = mode === 'none' ? text : text.replace(EMAIL, '[email]');
  return mode === 'email-phone' ? withoutEmail.replace(PHONE, '[phone]') : withoutEmail;
}
