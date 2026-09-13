/**
 * Website-authoritative legal pages. Keep in sync with
 * bgssai-media-admin/frontend/src/legal/catalog.ts and
 * docs/feature/legal-fan-out.md.
 *
 * Host and slugs must match https://www.bgssai.com published pages EXACTLY.
 * Do not invent /legal/* hosts or slugs.
 */
export const LEGAL_REVIEW_STATUS = '需法务审阅';

export const WEBSITE_ORIGIN = 'https://www.bgssai.com';

export type LegalPage = {
  id: string;
  slug: string;
  labelZh: string;
  labelEn: string;
  hrefZh: string;
  hrefEn: string;
};

export const LEGAL_PAGES: readonly LegalPage[] = [
  {
    id: 'terms-of-service',
    slug: 'terms-of-service',
    labelZh: '服务条款',
    labelEn: 'Terms of Service',
    hrefZh: 'https://www.bgssai.com/terms-of-service/',
    hrefEn: 'https://www.bgssai.com/en/terms-of-service/',
  },
  {
    id: 'privacy-policy',
    slug: 'privacy-policy',
    labelZh: '隐私政策',
    labelEn: 'Privacy Policy',
    hrefZh: 'https://www.bgssai.com/privacy-policy/',
    hrefEn: 'https://www.bgssai.com/en/privacy-policy/',
  },
  {
    id: 'personal-information-inventory',
    slug: 'personal-information-inventory',
    labelZh: '个人信息收集清单',
    labelEn: 'Personal Information Inventory',
    hrefZh: 'https://www.bgssai.com/personal-information-inventory/',
    hrefEn: 'https://www.bgssai.com/en/personal-information-inventory/',
  },
  {
    id: 'third-party-sharing',
    slug: 'third-party-sharing',
    labelZh: '第三方信息共享清单',
    labelEn: 'Third-party Sharing List',
    hrefZh: 'https://www.bgssai.com/third-party-sharing/',
    hrefEn: 'https://www.bgssai.com/en/third-party-sharing/',
  },
  {
    id: 'app-permissions',
    slug: 'app-permissions',
    labelZh: '应用权限说明',
    labelEn: 'App Permissions',
    hrefZh: 'https://www.bgssai.com/app-permissions/',
    hrefEn: 'https://www.bgssai.com/en/app-permissions/',
  },
];

export function legalHrefZh(page: LegalPage): string {
  return page.hrefZh;
}

export function legalHrefEn(page: LegalPage): string {
  return page.hrefEn;
}

export function bilingualLabel(page: LegalPage): string {
  return `${page.labelZh} / ${page.labelEn}`;
}
