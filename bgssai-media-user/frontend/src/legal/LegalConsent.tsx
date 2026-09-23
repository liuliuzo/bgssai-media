import { LEGAL_PRIVACY, LEGAL_TERMS, legalHrefZh } from './catalog';

// 用户端登录卡内的法务同意行：只有这一句、只有两处链接。
// 口径见 bgssai-skeleton docs/BGSSAI-Standards.md 第 15.1 节；Chat 回调页与管理端不放。
// 书名号《》属于文案，链接文字里带着；中文界面只渲染中文句，不做中英并列。
export default function LegalConsent() {
  return (
    <p className="legal-consent">
      登录或注册即表示已阅读并同意
      <a href={legalHrefZh(LEGAL_TERMS)} target="_blank" rel="noopener noreferrer">
        《{LEGAL_TERMS.labelZh}》
      </a>
      与
      <a href={legalHrefZh(LEGAL_PRIVACY)} target="_blank" rel="noopener noreferrer">
        《{LEGAL_PRIVACY.labelZh}》
      </a>
    </p>
  );
}
