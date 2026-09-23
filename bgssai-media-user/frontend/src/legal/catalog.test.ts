import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';
import {
  COMPANY_NAME,
  LEGAL_PAGES,
  LEGAL_PRIVACY,
  LEGAL_TERMS,
  WEBSITE_ORIGIN,
  legalHrefEn,
  legalHrefZh,
} from './catalog.ts';

// 口径：bgssai-skeleton docs/BGSSAI-Standards.md 第 15 节（登录同意行与页脚统一口径）
const here = dirname(fileURLToPath(import.meta.url));
const ADMIN_LEGAL_DIR = resolve(here, '../../../../bgssai-media-admin/frontend/src/legal');
const USER_SRC_DIR = resolve(here, '..');
const ADMIN_SRC_DIR = resolve(ADMIN_LEGAL_DIR, '..');

// 本仓没有备案号，界面与数据里不许出现任何证照 / 备案编号
const LICENSE_PATTERN =
  /ICP|网文|视听|增值电信|许可证号|备\d{8,}|[A-Z]{2,}\d{6,}/;
// 第 15.3 节：内部备注不上界面，也不通过 data-* 带到 DOM
const INTERNAL_NOTE_PATTERN =
  /需法务审阅|待法务审阅|法务正文以官网为准|Needs legal review|legal review|data-legal-review|LEGAL_REVIEW/;
// 第 15.2 节：这三份文件只在官网法律信息中心与隐私政策正文互链，产品端不罗列
const REMOVED_PAGE_PATTERN =
  /个人信息收集清单|第三方信息共享清单|应用权限说明|Personal Information Inventory|Third-party Sharing|App Permissions/;

const AUTHORITATIVE = [
  {
    id: 'terms-of-service',
    labelZh: '用户协议',
    zh: 'https://www.bgssai.com/terms-of-service/',
    en: 'https://www.bgssai.com/en/terms-of-service/',
  },
  {
    id: 'privacy-policy',
    labelZh: '隐私政策',
    zh: 'https://www.bgssai.com/privacy-policy/',
    en: 'https://www.bgssai.com/en/privacy-policy/',
  },
] as const;

const USER_CONSENT = resolve(here, 'LegalConsent.tsx');
const USER_FOOTER = resolve(here, 'LegalFooter.tsx');
const ADMIN_FOOTER = resolve(ADMIN_LEGAL_DIR, 'LegalFooter.tsx');

function read(file: string): string {
  return readFileSync(file, 'utf8');
}

test('catalog keeps exactly the two product-side legal pages', () => {
  assert.equal(LEGAL_PAGES.length, 2);
  assert.equal(LEGAL_TERMS.id, 'terms-of-service');
  assert.equal(LEGAL_PRIVACY.id, 'privacy-policy');
  assert.equal(WEBSITE_ORIGIN, 'https://www.bgssai.com');
  assert.equal(COMPANY_NAME, '昆山兵贵神速智能科技有限公司');
});

test('each page matches www.bgssai.com authoritative slugs and /en/ variants', () => {
  const ids = new Set<string>();
  LEGAL_PAGES.forEach((page, index) => {
    const expected = AUTHORITATIVE[index];
    assert.equal(ids.has(page.id), false);
    ids.add(page.id);
    assert.equal(page.id, expected.id);
    assert.equal(page.slug, expected.id);
    assert.equal(page.labelZh, expected.labelZh);
    assert.equal(page.hrefZh, expected.zh);
    assert.equal(page.hrefEn, expected.en);
    assert.equal(legalHrefZh(page), expected.zh);
    assert.equal(legalHrefEn(page), expected.en);
    assert.ok(page.labelEn.length > 0);
    assert.equal(page.labelZh === page.labelEn, false);
    assert.match(page.hrefZh, /^https:\/\/www\.bgssai\.com\/[a-z0-9-]+\/$/);
    assert.match(page.hrefEn, /^https:\/\/www\.bgssai\.com\/en\/[a-z0-9-]+\/$/);
  });
});

test('catalog rejects wrong hosts and /legal/* slugs', () => {
  const blob = JSON.stringify({ COMPANY_NAME, WEBSITE_ORIGIN, LEGAL_PAGES });
  assert.equal(blob.includes('/legal/'), false);
  assert.equal(blob.includes('https://bgssai.com/'), false);
  assert.equal(blob.includes('http://www.bgssai.com'), false);
  assert.equal(blob.includes('/legal/terms'), false);
  assert.equal(blob.includes('/legal/privacy'), false);
  assert.equal(blob.includes('/legal/cookies'), false);
  assert.equal(blob.includes('/legal/legal-notice'), false);
});

test('catalog invents no license or filing numbers', () => {
  const blob = JSON.stringify({ COMPANY_NAME, WEBSITE_ORIGIN, LEGAL_PAGES });
  assert.equal(LICENSE_PATTERN.test(blob), false);
});

test('catalog exports no removed pages and no internal review notes', () => {
  const blob = JSON.stringify({ COMPANY_NAME, WEBSITE_ORIGIN, LEGAL_PAGES });
  assert.equal(REMOVED_PAGE_PATTERN.test(blob), false);
  assert.equal(INTERNAL_NOTE_PATTERN.test(blob), false);
});

test('consent line and footers link only terms and privacy via zh href helper', () => {
  for (const file of [USER_CONSENT, USER_FOOTER, ADMIN_FOOTER]) {
    const src = read(file);
    assert.match(src, /legalHrefZh/);
    assert.match(src, /LEGAL_TERMS/);
    assert.match(src, /LEGAL_PRIVACY/);
    assert.match(src, /target="_blank"/);
    assert.match(src, /rel="noopener noreferrer"/);
    assert.equal(src.includes('/legal/'), false);
    assert.equal(src.includes('LEGAL_PAGES.map'), false);
    assert.equal(INTERNAL_NOTE_PATTERN.test(src), false);
    assert.equal(REMOVED_PAGE_PATTERN.test(src), false);
    assert.equal(LICENSE_PATTERN.test(src), false);
    assert.equal(/labelEn|legalHrefEn/.test(src), false);
  }
});

test('user consent line is the single Standards sentence', () => {
  const src = read(USER_CONSENT);
  assert.match(src, /登录或注册即表示已阅读并同意/);
  assert.match(src, /《\{LEGAL_TERMS\.labelZh\}》/);
  assert.match(src, /《\{LEGAL_PRIVACY\.labelZh\}》/);
  assert.equal(src.includes('登录即表示'), false);
});

test('footers render © year company · terms · privacy with a live year', () => {
  for (const file of [USER_FOOTER, ADMIN_FOOTER]) {
    const src = read(file);
    assert.match(src, /new Date\(\)\.getFullYear\(\)/);
    assert.match(src, /© \{year\} \{COMPANY_NAME\}/);
    assert.match(src, /> · </);
    assert.equal(/20\d\d/.test(src), false);
  }
  assert.equal(read(USER_FOOTER), read(ADMIN_FOOTER));
});

test('footer is mounted at App level on both ends and nowhere else', () => {
  for (const dir of [USER_SRC_DIR, ADMIN_SRC_DIR]) {
    const app = read(resolve(dir, 'App.tsx'));
    assert.match(app, /<LegalFooter \/>/);
    assert.match(app, /<AppRouter \/>/);
  }
  for (const file of [
    resolve(USER_SRC_DIR, 'components/Layout/AppLayout.tsx'),
    resolve(USER_SRC_DIR, 'pages/LoginPage.tsx'),
    resolve(USER_SRC_DIR, 'pages/ChatCallbackPage.tsx'),
    resolve(ADMIN_SRC_DIR, 'components/AdminLayout.tsx'),
    resolve(ADMIN_SRC_DIR, 'pages/Login.tsx'),
  ]) {
    const src = read(file);
    assert.equal(src.includes('LegalFooter'), false);
    assert.equal(src.includes('LegalLinks'), false);
    assert.equal(INTERNAL_NOTE_PATTERN.test(src), false);
  }
});

test('user login page carries the consent line; admin login and callback carry none', () => {
  const userLogin = read(resolve(USER_SRC_DIR, 'pages/LoginPage.tsx'));
  assert.match(userLogin, /<LegalConsent \/>/);
  const adminLogin = read(resolve(ADMIN_SRC_DIR, 'pages/Login.tsx'));
  assert.equal(/Legal|bgssai\.com|已阅读并同意|隐私政策|用户协议/.test(adminLogin), false);
  const callback = read(resolve(USER_SRC_DIR, 'pages/ChatCallbackPage.tsx'));
  assert.equal(/LegalConsent|已阅读并同意/.test(callback), false);
});

test('user and admin catalogs stay identical', () => {
  const normalize = (src: string) =>
    src
      .replace(/bgssai-media-admin\/frontend/g, 'PEER')
      .replace(/bgssai-media-user\/frontend/g, 'PEER');
  assert.equal(
    normalize(read(resolve(here, 'catalog.ts'))),
    normalize(read(resolve(ADMIN_LEGAL_DIR, 'catalog.ts'))),
  );
});
