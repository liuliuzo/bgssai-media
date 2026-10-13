
interface LegalConsentProps {
  checked: boolean
  onChange: (checked: boolean) => void
}

export default function LegalConsent({ checked, onChange }: LegalConsentProps) {
  return (
    <label className="legal-consent">
      <input type="checkbox" checked={checked} onChange={(e) => onChange(e.target.checked)} />
      <span>
        我已阅读并同意
        <a href="https://www.bgssai.com/terms-of-service/" target="_blank" rel="noopener noreferrer" onClick={(e) => e.stopPropagation()}>《用户协议》</a>
        与
        <a href="https://www.bgssai.com/privacy-policy/" target="_blank" rel="noopener noreferrer" onClick={(e) => e.stopPropagation()}>《隐私政策》</a>
        。
      </span>
    </label>
  )
}
