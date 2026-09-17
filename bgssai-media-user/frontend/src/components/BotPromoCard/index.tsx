import { useState } from 'react';
import { Link } from 'react-router-dom';
import './BotPromoCard.css';

const STORAGE_KEY = 'bgssai-media-bot-promo-dismissed';

function readDismissed(): boolean {
  try {
    return localStorage.getItem(STORAGE_KEY) === '1';
  } catch {
    return false;
  }
}

function BotMark() {
  return (
    <svg
      className="bot-promo-card__mark"
      viewBox="0 0 64 64"
      aria-hidden="true"
      focusable="false"
    >
      <circle cx="32" cy="32" r="32" fill="#111" />
      <rect x="18" y="24" width="10" height="18" rx="5" fill="#fff" transform="rotate(-18 23 33)" />
      <rect x="36" y="24" width="10" height="18" rx="5" fill="#fff" transform="rotate(-18 41 33)" />
    </svg>
  );
}

export default function BotPromoCard() {
  const [visible, setVisible] = useState(() => !readDismissed());

  if (!visible) return null;

  const onDismiss = () => {
    try {
      localStorage.setItem(STORAGE_KEY, '1');
    } catch {
      /* ignore quota / private mode */
    }
    setVisible(false);
  };

  return (
    <aside className="bot-promo-card" aria-label="认识 BGSSAI Bot">
      <div className="bot-promo-card__visual">
        <BotMark />
      </div>
      <div className="bot-promo-card__body">
        <h2 className="bot-promo-card__title">认识 BGSSAI Bot</h2>
        <p className="bot-promo-card__blurb">
          可委派实际工作的 AI 队友。Bot 能登录你的工具，像你一样使用它们，并带回已完成的工作。
        </p>
      </div>
      <div className="bot-promo-card__actions">
        <button type="button" className="bot-promo-card__dismiss" onClick={onDismiss}>
          忽略
        </button>
        <Link to="/download/bot" className="bot-promo-card__download">
          下载 BGSSAI Bot
        </Link>
      </div>
    </aside>
  );
}
