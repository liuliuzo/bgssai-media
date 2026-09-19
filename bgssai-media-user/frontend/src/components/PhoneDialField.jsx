import { DEFAULT_DIAL_CODE, DIAL_CODES, digitsOnly, nationalMaxLength, nationalPlaceholder, toDialItems } from '../lib/phoneDial'
import PhoneDialSearchSelect from './PhoneDialSearchSelect'

export function PhoneDialField({
  id,
  dialCode,
  nationalNumber,
  onDialCodeChange,
  onNationalNumberChange,
  disabled,
  wrapperClassName = 'phone-dial-field',
  selectClassName = 'phone-dial-select',
  inputClassName = 'phone-dial-number',
  ariaLabel = '国家区号',
  searchPlaceholder = '搜索国家或区号',
  items,
}) {
  const currentDial = dialCode || DEFAULT_DIAL_CODE

  function handleNationalChange(raw) {
    let next = digitsOnly(raw)
    const cc = digitsOnly(currentDial)
    if (cc && next.startsWith(cc) && next.length > cc.length + 6) {
      next = next.slice(cc.length)
    }
    onNationalNumberChange(next)
  }

  return (
    <div className={wrapperClassName}>
      <PhoneDialSearchSelect
        id={id ? `${id}-dial` : undefined}
        className={selectClassName}
        value={currentDial}
        disabled={disabled}
        ariaLabel={ariaLabel}
        searchPlaceholder={searchPlaceholder}
        items={items || toDialItems(DIAL_CODES)}
        onChange={onDialCodeChange}
      />
      <span className="phone-dial-divider" aria-hidden="true" />
      <input
        id={id}
        className={inputClassName}
        type="tel"
        inputMode="numeric"
        autoComplete="tel-national"
        maxLength={nationalMaxLength(currentDial)}
        placeholder={nationalPlaceholder(currentDial)}
        value={nationalNumber}
        disabled={disabled}
        onChange={(e) => handleNationalChange(e.target.value)}
      />
    </div>
  )
}

export default PhoneDialField
