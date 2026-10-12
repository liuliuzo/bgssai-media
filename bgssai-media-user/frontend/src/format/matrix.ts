export type WebDecision = 'PLAY' | 'REJECT' | 'UNKNOWN';

export interface MatrixRow {
  surface?: string;
  kind?: string;
  id?: string;
  label?: string;
  extensions?: string[];
  declared?: string;
  verification?: string;
  web_decision?: string;
  evidence?: string;
}

export interface MatrixDocument {
  verified_rows?: MatrixRow[];
  deployment?: {
    status?: string;
    media_user_host?: string;
    media_admin_host?: string;
    note?: string;
  };
}

export interface FormatHook {
  extension: string;
  web_decision: WebDecision;
  verification: string;
  declared: string;
  matched_surface: string | null;
  row_id: string | null;
  label: string | null;
}

/**
 * Web playback decision from the verified format table.
 * Lockstep with FormatMatrix.java and desktop shared/format-hook.js:
 * .mp4 PLAY, .mkv REJECT, .m3u8 PLAY, no extension UNKNOWN.
 */
export function extensionOf(input: string | null | undefined): string {
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

function findContainer(document: MatrixDocument, surface: string, extension: string): MatrixRow | null {
  const rows = Array.isArray(document.verified_rows) ? document.verified_rows : [];
  for (const row of rows) {
    if (!row || row.surface !== surface || row.kind !== 'container') continue;
    const extensions = Array.isArray(row.extensions) ? row.extensions : [];
    if (extensions.some((ext) => String(ext).toLowerCase() === extension)) return row;
  }
  return null;
}

export function webPlayback(input: string | null | undefined, document: MatrixDocument): FormatHook {
  const extension = extensionOf(input);
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
  const web = findContainer(document, 'web', extension);
  const row = web || findContainer(document, 'desktop_vlc', extension);
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
  const decision = row.web_decision;
  const webDecision: WebDecision =
    decision === 'PLAY' || decision === 'REJECT' || decision === 'UNKNOWN' ? decision : 'UNKNOWN';
  return {
    extension,
    web_decision: webDecision,
    verification: row.verification || 'NOT_VERIFIED',
    declared: row.declared || 'UNKNOWN',
    matched_surface: row.surface || null,
    row_id: row.id || null,
    label: row.label || null,
  };
}
