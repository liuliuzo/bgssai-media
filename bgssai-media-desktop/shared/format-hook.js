'use strict';

/**
 * Desktop copy of the format-matrix URL hook.
 * Lockstep with FormatMatrix.java and frontend/src/format/matrix.ts:
 * .mp4 PLAY, .mkv REJECT, .m3u8 PLAY, no extension UNKNOWN.
 */
function extensionOf(input) {
  if (input == null) return '';
  let clean = String(input).trim();
  const query = clean.indexOf('?');
  if (query >= 0) clean = clean.slice(0, query);
  const hash = clean.indexOf('#');
  if (hash >= 0) clean = clean.slice(0, hash);
  const slash = clean.lastIndexOf('/');
  const name = slash >= 0 ? clean.slice(slash + 1) : clean;
  const dot = name.lastIndexOf('.');
  if (dot < 0 || dot === name.length - 1) return '';
  return name.slice(dot).toLowerCase();
}

function findContainer(matrix, surface, extension) {
  const rows = Array.isArray(matrix && matrix.verified_rows) ? matrix.verified_rows : [];
  for (const row of rows) {
    if (!row || row.surface !== surface || row.kind !== 'container') continue;
    const extensions = Array.isArray(row.extensions) ? row.extensions : [];
    if (extensions.some((ext) => String(ext).toLowerCase() === extension)) return row;
  }
  return null;
}

function hook(matrix, mediaUrl) {
  const extension = extensionOf(mediaUrl);
  if (!extension) {
    return {
      extension,
      web_decision: 'UNKNOWN',
      verification: 'NOT_VERIFIED',
      declared: 'UNKNOWN',
      matched_surface: null,
      row_id: null,
      label: null,
    };
  }
  const web = findContainer(matrix, 'web', extension);
  const row = web || findContainer(matrix, 'desktop_vlc', extension);
  if (!row) {
    return {
      extension,
      web_decision: 'UNKNOWN',
      verification: 'NOT_VERIFIED',
      declared: 'UNKNOWN',
      matched_surface: null,
      row_id: null,
      label: null,
    };
  }
  return {
    extension,
    web_decision: row.web_decision || 'UNKNOWN',
    verification: row.verification || 'NOT_VERIFIED',
    declared: row.declared || 'UNKNOWN',
    matched_surface: row.surface,
    row_id: row.id,
    label: row.label,
  };
}

module.exports = { extensionOf, hook };
