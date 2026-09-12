import {
  LEGAL_PAGES,
  LEGAL_REVIEW_STATUS,
  legalHrefEn,
  legalHrefZh,
} from './catalog';

type LegalLinksVariant = 'user-consent' | 'footer';

type Props = {
  variant: LegalLinksVariant;
};

export default function LegalLinks({ variant }: Props) {
  const intro =
    variant === 'user-consent'
      ? '登录即表示已阅读并同意官网服务条款与隐私政策。法务正文以官网为准。'
      : '法务正文以官网为准。本产品不展示证照编号。';

  return (
    <div
      className={variant === 'footer' ? 'legal-strip legal-strip--footer' : 'legal-strip'}
      data-legal-review="pending"
    >
      <p className="legal-strip__intro">{intro}</p>
      <nav className="legal-strip__nav" aria-label="官网法务页 Legal pages">
        {LEGAL_PAGES.map((page, index) => (
          <span key={page.id}>
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
