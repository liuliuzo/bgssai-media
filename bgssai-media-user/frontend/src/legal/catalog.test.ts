import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';
import {
  LEGAL_PAGES,
  LEGAL_REVIEW_STATUS,
  WEBSITE_ORIGIN,
  bilingualLabel,
  legalHrefEn,
  legalHrefZh,
} from './catalog.ts';

const here = dirname(fileURLToPath(import.meta.url));
const LICENSE_PATTERN =
  /ICP|网文|视听|增值电信|许可证号|备\d{8,}|[A-Z]{2,}\d{6,}/;

const AUTHORITATIVE = [
  {
    id: 'terms-of-service',
    zh: 'https://www.bgssai.com/terms-of-service/',
    en: 'https://www.bgssai.com/en/terms-of-service/',
  },
  {
    id: 'privacy-policy',
    zh: 'https://www.bgssai.com/privacy-policy/',
    en: 'https://www.bgssai.com/en/privacy-policy/',
  },
  {
    id: 'personal-information-inventory',
    zh: 'https://www.bgssai.com/personal-information-inventory/',
    en: 'https://www.bgssai.com/en/personal-information-inventory/',
  },
  {
    id: 'third-party-sharing',
    zh: 'https://www.bgssai.com/third-party-sharing/',
    en: 'https://www.bgssai.com/en/third-party-sharing/',
  },
  {
    id: 'app-permissions',
    zh: 'https://www.bgssai.com/app-permissions/',
    en: 'https://www.bgssai.com/en/app-permissions/',
  },
] as const;

test('catalog has exactly 5 website legal pages', () => {
  assert.equal(LEGAL_PAGES.length, 5);
  assert.equal(LEGAL_REVIEW_STATUS, '需法务审阅');
  assert.equal(WEBSITE_ORIGIN, 'https://www.bgssai.com');
});

test('each page matches www.bgssai.com authoritative slugs and /en/ variants', () => {
  const ids = new Set<string>();
  LEGAL_PAGES.forEach((page, index) => {
    const expected = AUTHORITATIVE[index];
    assert.equal(ids.has(page.id), false);
    ids.add(page.id);
    assert.equal(page.id, expected.id);
    assert.equal(page.slug, expected.id);
    assert.equal(page.hrefZh, expected.zh);
    assert.equal(page.hrefEn, expected.en);
    assert.equal(legalHrefZh(page), expected.zh);
    assert.equal(legalHrefEn(page), expected.en);
    assert.equal(bilingualLabel(page), `${page.labelZh} / ${page.labelEn}`);
    assert.ok(page.labelZh.length > 0);
    assert.ok(page.labelEn.length > 0);
    assert.equal(page.labelZh === page.labelEn, false);
    assert.match(page.hrefZh, /^https:\/\/www\.bgssai\.com\/[a-z0-9-]+\/$/);
    assert.match(page.hrefEn, /^https:\/\/www\.bgssai\.com\/en\/[a-z0-9-]+\/$/);
  });
});

test('catalog rejects wrong hosts and /legal/* slugs', () => {
  const blob = JSON.stringify({ LEGAL_REVIEW_STATUS, WEBSITE_ORIGIN, LEGAL_PAGES });
  assert.equal(blob.includes('/legal/'), false);
  assert.equal(blob.includes('https://bgssai.com/'), false);
  assert.equal(blob.includes('http://www.bgssai.com'), false);
  assert.equal(blob.includes('/legal/terms'), false);
  assert.equal(blob.includes('/legal/privacy'), false);
  assert.equal(blob.includes('/legal/cookies'), false);
  assert.equal(blob.includes('/legal/legal-notice'), false);
});

test('catalog invents no license or filing numbers', () => {
  const blob = JSON.stringify({ LEGAL_REVIEW_STATUS, WEBSITE_ORIGIN, LEGAL_PAGES });
  assert.equal(LICENSE_PATTERN.test(blob), false);
});

test('link modules use zh and en href helpers, not /legal/*', () => {
  const files = [
    resolve(here, 'LegalLinks.tsx'),
    resolve(here, '../../../../bgssai-media-admin/frontend/src/legal/LegalLinks.tsx'),
  ];
  for (const file of files) {
    const src = readFileSync(file, 'utf8');
    assert.match(src, /legalHrefZh/);
    assert.match(src, /legalHrefEn/);
    assert.equal(src.includes('/legal/'), false);
    assert.equal(src.includes('/legal/terms'), false);
    assert.equal(src.includes('/legal/privacy'), false);
  }
});

test('user and admin catalogs stay identical', () => {
  const userSrc = readFileSync(resolve(here, 'catalog.ts'), 'utf8');
  const adminSrc = readFileSync(
    resolve(here, '../../../../bgssai-media-admin/frontend/src/legal/catalog.ts'),
    'utf8',
  );
  const normalize = (src: string) =>
    src
      .replace(/bgssai-media-admin\/frontend/g, 'PEER')
      .replace(/bgssai-media-user\/frontend/g, 'PEER');
  assert.equal(normalize(userSrc), normalize(adminSrc));
});
