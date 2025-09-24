import DOMPurify from 'dompurify';

// Sanitize input strings
export const sanitizeInput = (input) => {
  if (typeof input !== 'string') return input;
  return DOMPurify.sanitize(input, {
    ALLOWED_TAGS: [], // No HTML tags allowed in input
    ALLOWED_ATTR: [], // No attributes allowed
  });
};

// Sanitize output HTML (for cases where we need to render HTML content)
export const sanitizeOutput = (html) => {
  if (typeof html !== 'string') return html;
  return DOMPurify.sanitize(html, {
    ALLOWED_TAGS: ['b', 'i', 'em', 'strong', 'a', 'p', 'br'],
    ALLOWED_ATTR: ['href', 'target', 'rel'],
    ALLOW_DATA_ATTR: false,
    ADD_ATTR: ['target="_blank"', 'rel="noopener noreferrer"'],
  });
};

// Sanitize URL
export const sanitizeUrl = (url) => {
  if (typeof url !== 'string') return url;
  try {
    const parsed = new URL(url);
    // Only allow http and https protocols
    if (!['http:', 'https:'].includes(parsed.protocol)) {
      return '';
    }
    return parsed.toString();
  } catch {
    return '';
  }
};

// Escape special characters in text content
export const escapeHtml = (text) => {
  if (typeof text !== 'string') return text;
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
};

// Email sanitization
export const sanitizeEmail = (email) => {
  if (typeof email !== 'string') return email;
  // First sanitize any HTML/script content
  const sanitized = sanitizeInput(email.toLowerCase().trim());
  // Validate email format
  return /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(sanitized) ? sanitized : '';
};

// Phone number sanitization
export const sanitizePhone = (phone) => {
  if (typeof phone !== 'string') return phone;
  // Remove any non-numeric characters except +, -, (, ), and space
  return phone.replace(/[^\d+\-() ]/g, '');
};

// Object sanitization for nested data
export const sanitizeObject = (obj) => {
  if (typeof obj !== 'object' || obj === null) return obj;
  
  const sanitized = Array.isArray(obj) ? [] : {};
  
  for (const [key, value] of Object.entries(obj)) {
    if (typeof value === 'object' && value !== null) {
      sanitized[key] = sanitizeObject(value);
    } else if (typeof value === 'string') {
      sanitized[key] = sanitizeInput(value);
    } else {
      sanitized[key] = value;
    }
  }
  
  return sanitized;
};

// Sanitize coordinates
export const sanitizeCoordinates = (coords) => {
  if (typeof coords !== 'object' || coords === null) return coords;
  
  const sanitized = {};
  if ('lat' in coords) {
    sanitized.lat = typeof coords.lat === 'number' ? coords.lat : parseFloat(sanitizeInput(String(coords.lat))) || 0;
  }
  if ('lng' in coords) {
    sanitized.lng = typeof coords.lng === 'number' ? coords.lng : parseFloat(sanitizeInput(String(coords.lng))) || 0;
  }
  if ('latitude' in coords) {
    sanitized.latitude = typeof coords.latitude === 'number' ? coords.latitude : parseFloat(sanitizeInput(String(coords.latitude))) || 0;
  }
  if ('longitude' in coords) {
    sanitized.longitude = typeof coords.longitude === 'number' ? coords.longitude : parseFloat(sanitizeInput(String(coords.longitude))) || 0;
  }
  
  return sanitized;
};

// Validate and sanitize image URLs
export const validateAndSanitizeImageUrl = (url) => {
  if (typeof url !== 'string') return '';
  const sanitized = sanitizeUrl(url);
  // Check if URL points to an image
  return sanitized.match(/\.(jpg|jpeg|png|gif|webp)(\?.*)?$/i) ? sanitized : '';
};