import { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import './phone-dial-search.css'

function normalize(value) {
  return String(value || '').toLowerCase().replace(/\s+/g, '')
}

function matchesQuery(item, query) {
  const q = normalize(query)
  if (!q) return true
  const hay = normalize(`${item.value} ${item.label} ${item.keywords || ''}`)
  return hay.includes(q) || hay.includes(q.replace(/^\+/, ''))
}

export default function PhoneDialSearchSelect({
  id,
  value,
  onChange,
  disabled,
  className = 'phone-dial-select',
  ariaLabel = '国家区号',
  items,
  searchPlaceholder = '搜索国家或区号',
}) {
  const [open, setOpen] = useState(false)
  const [query, setQuery] = useState('')
  const [active, setActive] = useState(0)
  const [pos, setPos] = useState({ top: 0, left: 0, width: 280 })
  const rootRef = useRef(null)
  const searchRef = useRef(null)
  const listRef = useRef(null)

  const filtered = useMemo(
    () => (items || []).filter((item) => matchesQuery(item, query)),
    [items, query],
  )

  const selected = (items || []).find((item) => item.value === value) || filtered[0]

  function placePanel() {
    const el = rootRef.current
    if (!el) return
    const rect = el.getBoundingClientRect()
    const width = Math.min(Math.max(rect.width, 280), 360)
    let left = rect.left
    if (left + width > window.innerWidth - 12) {
      left = Math.max(12, window.innerWidth - width - 12)
    }
    const below = rect.bottom + 6
    const panelHeight = 320
    const top = below + panelHeight > window.innerHeight - 12
      ? Math.max(12, rect.top - panelHeight - 6)
      : below
    setPos({ top, left, width })
  }

  useLayoutEffect(() => {
    if (!open) return undefined
    placePanel()
    const onReposition = () => placePanel()
    window.addEventListener('resize', onReposition)
    window.addEventListener('scroll', onReposition, true)
    return () => {
      window.removeEventListener('resize', onReposition)
      window.removeEventListener('scroll', onReposition, true)
    }
  }, [open])

  useEffect(() => {
    if (!open) return undefined
    setQuery('')
    const index = Math.max(0, filtered.findIndex((item) => item.value === value))
    setActive(index)
    const timer = window.setTimeout(() => {
      if (searchRef.current) searchRef.current.focus()
    }, 0)
    return () => window.clearTimeout(timer)
  }, [open])

  useEffect(() => {
    if (!open) return undefined
    function onDoc(event) {
      const root = rootRef.current
      const panel = listRef.current
      if (root && root.contains(event.target)) return
      if (panel && panel.contains(event.target)) return
      setOpen(false)
    }
    document.addEventListener('mousedown', onDoc)
    return () => document.removeEventListener('mousedown', onDoc)
  }, [open])

  function choose(item) {
    if (!item) return
    onChange(item.value)
    setOpen(false)
  }

  function onTriggerKey(event) {
    if (disabled) return
    if (event.key === 'ArrowDown' || event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      setOpen(true)
    }
  }

  function onSearchKey(event) {
    if (event.key === 'Escape') {
      event.preventDefault()
      setOpen(false)
      return
    }
    if (event.key === 'ArrowDown') {
      event.preventDefault()
      setActive((n) => Math.min(filtered.length - 1, n + 1))
      return
    }
    if (event.key === 'ArrowUp') {
      event.preventDefault()
      setActive((n) => Math.max(0, n - 1))
      return
    }
    if (event.key === 'Enter') {
      event.preventDefault()
      choose(filtered[active])
    }
  }

  return (
    <div className={`phone-dial-combo ${className}`.trim()} ref={rootRef}>
      <button
        id={id}
        type="button"
        className="phone-dial-combo-trigger"
        disabled={disabled}
        aria-label={ariaLabel}
        aria-haspopup="listbox"
        aria-expanded={open}
        onClick={() => !disabled && setOpen((v) => !v)}
        onKeyDown={onTriggerKey}
      >
        <span className="phone-dial-combo-code">{value}</span>
        <span className="phone-dial-combo-caret" aria-hidden="true" />
      </button>
      {open
        ? createPortal(
            <div
              ref={listRef}
              className="phone-dial-combo-panel"
              style={{ position: 'fixed', top: pos.top, left: pos.left, width: pos.width, zIndex: 4000 }}
            >
              <input
                ref={searchRef}
                className="phone-dial-combo-search"
                type="search"
                value={query}
                placeholder={searchPlaceholder}
                aria-label={searchPlaceholder}
                onChange={(e) => {
                  setQuery(e.target.value)
                  setActive(0)
                }}
                onKeyDown={onSearchKey}
              />
              <ul className="phone-dial-combo-list" role="listbox" aria-label={ariaLabel}>
                {filtered.length === 0 ? (
                  <li className="phone-dial-combo-empty">没有匹配的区号</li>
                ) : (
                  filtered.map((item, index) => {
                    const name = String(item.label || '').replace(item.value, '').trim()
                    return (
                      <li key={item.value}>
                        <button
                          type="button"
                          role="option"
                          aria-selected={item.value === value}
                          className={`phone-dial-combo-option${item.value === value ? ' is-selected' : ''}${index === active ? ' is-active' : ''}`}
                          onMouseEnter={() => setActive(index)}
                          onClick={() => choose(item)}
                        >
                          <span className="phone-dial-combo-option-dial">{item.value}</span>
                          <span className="phone-dial-combo-option-name">{name || item.label}</span>
                        </button>
                      </li>
                    )
                  })
                )}
              </ul>
            </div>,
            document.body,
          )
        : null}
      {selected ? <span className="sr-only">{selected.label}</span> : null}
    </div>
  )
}
