import {
  LEGAL_PAGES,
  LEGAL_REVIEW_STATUS,
  legalHrefEn,
  legalHrefZh,
} from './catalog';

type LegalLinksVariant = 'admin-outbound' | 'footer';

type Props = {
  variant: LegalLinksVariant;
};

export default function LegalLinks({ variant }: Props) {
  const intro =
    variant === 'admin-outbound'
      ? '管理端仅外链至官网法务页，不承载用户授权或法务正文。'
      : '法务正文以官网为准。管理端仅外链，不展示证照编号。';

  return (
    <div
      className={variant === 'footer' ? 'legal-strip legal-strip--footer' : 'legal-strip'}
      data-legal-review="pending"
    >
      <p className="legal-strip__intro">{intro}</p>
      <nav className="legal-strip__nav" aria-label="官网法务页 Legal pages">
        {LEGAL_PAGES.map((page, index) => (
          <span key={page.id} className="legal-strip__item">
            {index > 0 ? <span className="legal-strip__sep" aria-hidden="true"> | </span> : null}
            <a
              href={legalHrefZh(page)}
              target="_blank"
              rel="noopener noreferrer"
              data-legal-id={page.id}
              data-legal-lang="zh"
            >
              {page.labelZh}
            </a>
            <span className="legal-strip__lang" aria-hidden="true">
              {' / '}
            </span>
            <a
              href={legalHrefEn(page)}
              target="_blank"
              rel="noopener noreferrer"
              data-legal-id={page.id}
              data-legal-lang="en"
            >
              {page.labelEn}
            </a>
          </span>
        ))}
      </nav>
      <p className="legal-strip__review">{LEGAL_REVIEW_STATUS}</p>
    </div>
  );
}
