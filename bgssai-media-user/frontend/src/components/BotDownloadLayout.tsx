import type { ReactNode } from 'react'
import { Link } from 'react-router-dom';
import './BotDownloadLayout.css'

export default function BotDownloadLayout({ children }: { children: ReactNode }) {


  return (
    <div className="bot-download-layout">
      <nav className="bot-download-nav" aria-label="BGSSAI Bot">
        <strong>BGSSAI Bot</strong>
        <Link to="/download/bot">下载 BGSSAI BOT</Link>
      </nav>
      <div className="bot-download-content">{children}</div>
    </div>
  )
}
