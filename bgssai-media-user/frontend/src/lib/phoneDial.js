export const DEFAULT_DIAL_CODE = '+86'

export const DIAL_CODES = [
  { dial: '+86', label: '中国大陆', keywords: 'China CN' },
  { dial: '+852', label: '中国香港', keywords: 'Hong Kong HK' },
  { dial: '+853', label: '中国澳门', keywords: 'Macau Macao MO' },
  { dial: '+886', label: '中国台湾', keywords: 'Taiwan TW' },
  { dial: '+1', label: '美国/加拿大', keywords: 'United States Canada US CA USA' },
  { dial: '+7', label: '俄罗斯', keywords: 'Russia RU' },
  { dial: '+33', label: '法国', keywords: 'France FR' },
  { dial: '+34', label: '西班牙', keywords: 'Spain ES' },
  { dial: '+39', label: '意大利', keywords: 'Italy IT' },
  { dial: '+44', label: '英国', keywords: 'United Kingdom UK GB' },
  { dial: '+49', label: '德国', keywords: 'Germany DE' },
  { dial: '+55', label: '巴西', keywords: 'Brazil BR' },
  { dial: '+60', label: '马来西亚', keywords: 'Malaysia MY' },
  { dial: '+61', label: '澳大利亚', keywords: 'Australia AU' },
  { dial: '+62', label: '印度尼西亚', keywords: 'Indonesia ID' },
  { dial: '+63', label: '菲律宾', keywords: 'Philippines PH' },
  { dial: '+64', label: '新西兰', keywords: 'New Zealand NZ' },
  { dial: '+65', label: '新加坡', keywords: 'Singapore SG' },
  { dial: '+66', label: '泰国', keywords: 'Thailand TH' },
  { dial: '+81', label: '日本', keywords: 'Japan JP' },
  { dial: '+82', label: '韩国', keywords: 'South Korea KR' },
  { dial: '+84', label: '越南', keywords: 'Vietnam VN' },
  { dial: '+90', label: '土耳其', keywords: 'Turkey TR' },
  { dial: '+91', label: '印度', keywords: 'India IN' },
  { dial: '+92', label: '巴基斯坦', keywords: 'Pakistan PK' },
  { dial: '+351', label: '葡萄牙', keywords: 'Portugal PT' },
  { dial: '+353', label: '爱尔兰', keywords: 'Ireland IE' },
  { dial: '+358', label: '芬兰', keywords: 'Finland FI' },
  { dial: '+380', label: '乌克兰', keywords: 'Ukraine UA' },
  { dial: '+420', label: '捷克', keywords: 'Czechia CZ' },
  { dial: '+855', label: '柬埔寨', keywords: 'Cambodia KH' },
  { dial: '+856', label: '老挝', keywords: 'Laos LA' },
  { dial: '+880', label: '孟加拉国', keywords: 'Bangladesh BD' },
  { dial: '+966', label: '沙特阿拉伯', keywords: 'Saudi Arabia SA' },
  { dial: '+971', label: '阿联酋', keywords: 'United Arab Emirates AE UAE' },
  { dial: '+974', label: '卡塔尔', keywords: 'Qatar QA' },
]

export function digitsOnly(value) {
  return String(value || '').replace(/\D/g, '')
}

export function composeApiPhone(dialCode, nationalNumber, format = 'cn-national') {
  const national = digitsOnly(nationalNumber)
  const cc = digitsOnly(dialCode) || '86'
  if (format === 'e164') return `+${cc}${national}`
  if (cc === '86') return national
  return `+${cc}${national}`
}

export function validatePhoneParts(dialCode, nationalNumber) {
  const national = digitsOnly(nationalNumber)
  if (!national) return '请输入手机号'
  const cc = digitsOnly(dialCode) || '86'
  if (cc === '86') {
    return /^1[3-9]\d{9}$/.test(national) ? null : '请输入正确的 11 位手机号'
  }
  if (national.length < 4 || national.length > 15) return '请输入正确的电话号码'
  return null
}

export function nationalMaxLength(dialCode) {
  return digitsOnly(dialCode) === '86' ? 11 : 15
}

export function nationalPlaceholder(dialCode) {
  return digitsOnly(dialCode) === '86' ? '请输入手机号' : '请输入电话号码'
}

export function emptyPhoneValue() {
  return { dial: DEFAULT_DIAL_CODE, national: '' }
}

export function composePhoneValue(value, format = 'cn-national') {
  return composeApiPhone(value?.dial || DEFAULT_DIAL_CODE, value?.national || '', format)
}

export function toDialItems(codes) {
  return (codes || []).map((item) => ({
    value: item.dial,
    label: `${item.dial} ${item.label || ''}`,
    keywords: item.keywords || item.label || '',
  }))
}
