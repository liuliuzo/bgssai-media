/**
 * Website-authoritative legal pages. Keep in sync with
 * bgssai-media-admin/frontend/src/legal/catalog.ts and
 * docs/feature/legal-fan-out.md.
 * Source checklist: bgssai-website develop docs/feature/legal-fan-out-checklist.md
 */
export const LEGAL_REVIEW_STATUS = '需法务审阅';

export const WEBSITE_ORIGIN = 'https://www.bgssai.com';

export type LegalPage = {
  id: string;
  path: string;
  labelZh: string;
  labelEn: string;
};

export const LEGAL_PAGES: readonly LegalPage[] = [
  {
    id: 'terms',
    path: '/legal/terms',
    labelZh: '服务条款',
    labelEn: 'Terms of Service',
  },
  {
    id: 'privacy',
    path: '/legal/privacy',
    labelZh: '隐私政策',
    labelEn: 'Privacy Policy',
  },
  {
    id: 'cookies',
    path: '/legal/cookies',
    labelZh: 'Cookie 政策',
    labelEn: 'Cookie Policy',
  },
  {
    id: 'third-party-sharing',
    path: '/legal/third-party-sharing',
    labelZh: '第三方信息共享清单',
    labelEn: 'Third-party Sharing List',
  },
  {
    id: 'legal-notice',
    path: '/legal/legal-notice',
    labelZh: '法律声明',
    labelEn: 'Legal Notice',
  },
];

export function legalHref(page: LegalPage): string {
  return `${WEBSITE_ORIGIN}${page.path}`;
}

export function bilingualLabel(page: LegalPage): string {
  return `${page.labelZh} / ${page.labelEn}`;
}
