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
  legalHref,
} from './catalog.ts';

const here = dirname(fileURLToPath(import.meta.url));
const LICENSE_PATTERN =
  /ICP|网文|视听|增值电信|许可证号|备\d{8,}|[A-Z]{2,}\d{6,}/;

test('catalog has exactly 5 website legal pages', () => {
  assert.equal(LEGAL_PAGES.length, 5);
  assert.equal(LEGAL_REVIEW_STATUS, '需法务审阅');
  assert.equal(WEBSITE_ORIGIN, 'https://www.bgssai.com');
});

test('each page is a bilingual outbound link to /legal/*', () => {
  const ids = new Set<string>();
  for (const page of LEGAL_PAGES) {
    assert.equal(ids.has(page.id), false);
    ids.add(page.id);
    assert.match(page.path, /^\/legal\/[a-z0-9-]+$/);
    assert.equal(legalHref(page), `${WEBSITE_ORIGIN}${page.path}`);
    assert.equal(bilingualLabel(page), `${page.labelZh} / ${page.labelEn}`);
    assert.ok(page.labelZh.length > 0);
    assert.ok(page.labelEn.length > 0);
    assert.equal(page.labelZh === page.labelEn, false);
  }
});

test('catalog invents no license or filing numbers', () => {
  const blob = JSON.stringify({ LEGAL_REVIEW_STATUS, WEBSITE_ORIGIN, LEGAL_PAGES });
  assert.equal(LICENSE_PATTERN.test(blob), false);
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
