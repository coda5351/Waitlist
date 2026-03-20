/**
 * Formats a date string to MM/DD/YYYY format
 * @param dateString - ISO date string or any valid date string
 * @returns Formatted date string in MM/DD/YYYY format
 */
export const formatDate = (dateString: string): string => {
  const normalized = normalizeDateString(dateString)
  const date = new Date(normalized)
  return date.toLocaleDateString('en-US', { 
    month: '2-digit', 
    day: '2-digit', 
    year: 'numeric' 
  })
}

/**
 * Formats a date string to DD/MM/YYYY HH:MM AM/PM format
 * @param dateString - ISO date string or any valid date string
 * @returns Formatted date and time string
 */
export const formatDateTime = (dateString: string): string => {
  const normalized = normalizeDateString(dateString)
  const date = new Date(normalized)
  const opts: Intl.DateTimeFormatOptions = {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
    hour12: true
  }
  return date.toLocaleString('en-US', opts)
}

/**
 * Extract just the time (hh:mm AM/PM) portion from a date string.
 */
const normalizeDateString = (dateString: string): string => {
  // Strip microseconds beyond millisecond precision (JS Date supports max 3 digits)
  return dateString.replace(/(\.\d{3})\d*/, '$1')
}

export const formatTime = (dateString: string): string => {
  const normalized = normalizeDateString(dateString)
  const date = new Date(normalized)
  const opts: Intl.DateTimeFormatOptions = {
    hour: 'numeric',
    minute: '2-digit',
    hour12: true
  }
  return date.toLocaleTimeString('en-US', opts)
}
