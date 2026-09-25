import './ProductMotion.css'

/** A decorative product illustration. No timers, network requests, or focusable elements. */
export default function ProductMotion() {
  return (
    <div className="product-motion" aria-hidden="true">
      <svg viewBox="0 0 320 120" focusable="false">
        <g fill="none" stroke="currentColor" strokeWidth="1">
          <ellipse className="pm-orbit" cx="160" cy="60" rx="111" ry="43" strokeDasharray="2 9" />
          <ellipse className="pm-orbit pm-orbit-inner" cx="160" cy="60" rx="78" ry="31" strokeDasharray="8 12" />
          <path className="pm-link" d="M38 78 C80 78 100 60 138 60 M182 60 C218 60 234 32 280 32" />
        </g>
        <g className="pm-node">
          <rect className="pm-tile" x="26" y="61" width="34" height="34" rx="10" />
          <path className="pm-glyph" d="M36 72h14 M36 78h10 M36 84h6" />
        </g>
        <g className="pm-core">
          <rect className="pm-tile" x="134" y="34" width="52" height="52" rx="16" />
          <g className="pm-glyph" transform="translate(148 48)">
            <path d="M3 5h18v14H3V5 M3 9h18 M7 5v4 M13 5v4 M17 5v4 M10 12l5 3-5 2v-5" />
          </g>
        </g>
        <g className="pm-node pm-node-last">
          <rect className="pm-tile" x="259" y="15" width="34" height="34" rx="10" />
          <path className="pm-glyph" d="M269 38v-8 M276 38V25 M283 38V29" />
        </g>
        <circle className="pm-dot" cx="98" cy="28" r="2" />
        <circle className="pm-dot" cx="224" cy="92" r="3" />
        <circle className="pm-dot" cx="76" cy="54" r="1.5" />
      </svg>
    </div>
  )
}
