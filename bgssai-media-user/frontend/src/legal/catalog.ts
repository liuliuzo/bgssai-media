/**
 * 官网法务页外链的唯一数据来源。与
 * bgssai-media-admin/frontend/src/legal/catalog.ts 保持逐字一致（catalog.test.ts 会比对），
 * 口径见 bgssai-skeleton docs/BGSSAI-Standards.md 第 15 节与 docs/feature/legal-fan-out.md。
 *
 * 产品端只链《用户协议》与《隐私政策》两页；「个人信息收集清单」「第三方信息共享清单」
 * 「应用权限说明」只在官网法律信息中心与隐私政策正文内互链，不在产品界面罗列。
 * host 与 slug 必须与 https://www.bgssai.com 已发布页面完全一致，不得自造路径。
 * 本仓没有备案号，页脚不放；备案通过后只补号，别的不动。
 * 「需法务审阅」之类内部备注只留在注释与 docs，不导出、不渲染。
 */
export const WEBSITE_ORIGIN = 'https://www.bgssai.com';

export const COMPANY_NAME = '昆山兵贵神速智能科技有限公司';

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
    labelZh: '用户协议',
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
];

export const LEGAL_TERMS: LegalPage = LEGAL_PAGES[0];

export const LEGAL_PRIVACY: LegalPage = LEGAL_PAGES[1];

export function legalHrefZh(page: LegalPage): string {
  return page.hrefZh;
}

export function legalHrefEn(page: LegalPage): string {
  return page.hrefEn;
}
