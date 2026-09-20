import { Link } from 'react-router-dom';
import './BotPromoCard.css';

/**
 * 登录卡下方的 BOT 下载入口，一行。
 * 原先是一张带标题和整段介绍的浮动卡片——登录页要的是尽快登进去，
 * 介绍性文字挪回 /download/bot，这里只留入口。不再写 localStorage。
 */
export default function BotPromoCard() {
  return (
    <p className="bot-promo-link">
      <Link to="/download/bot">下载 BGSSAI BOT</Link>
    </p>
  );
}
