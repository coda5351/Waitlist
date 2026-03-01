// simple phone formatter and link helper
// Formats digits into nice string and provides tel: link value

export function formatPhoneNumber(raw: string | null | undefined): string {
  if (!raw) return '';
  // remove non-digit
  const digits = raw.replace(/\D/g, '');
  if (digits.length === 10) {
    return digits.replace(/(\d{3})(\d{3})(\d{4})/, '($1) $2-$3');
  }
  // fallback
  return raw;
}

export function phoneLink(raw: string | null | undefined): string {
  if (!raw) return '';
  const digits = raw.replace(/\D/g, '');
  return `tel:${digits}`;
}
