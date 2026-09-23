import { COMPANY_NAME, LEGAL_PRIVACY, LEGAL_TERMS, legalHrefZh } from './catalog';

// App 级一行页脚，挂在 App.tsx 根节点上，覆盖含 /login 在内的所有路由。
// 口径见 bgssai-skeleton docs/BGSSAI-Standards.md 第 15.2 节：© 年份 公司 · 用户协议 · 隐私政策（· 备案号）。
// 本仓没有备案号，不编、不借别的产品的；年份取当年，不写死。
export default function LegalFooter() {
  const year = new Date().getFullYear();
  return (
    <footer className="legal-footer">
      <span className="legal-footer__owner">© {year} {COMPANY_NAME}</span>
      <span className="legal-footer__sep" aria-hidden="true"> · </span>
      <a href={legalHrefZh(LEGAL_TERMS)} target="_blank" rel="noopener noreferrer">
        {LEGAL_TERMS.labelZh}
      </a>
      <span className="legal-footer__sep" aria-hidden="true"> · </span>
      <a href={legalHrefZh(LEGAL_PRIVACY)} target="_blank" rel="noopener noreferrer">
        {LEGAL_PRIVACY.labelZh}
      </a>
    </footer>
  );
}
