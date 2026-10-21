import { useState } from 'react'

export default function LoginModal({ open, onClose, onLogin }) {
  const [email, setEmail] = useState('demo@careerpick.kr')
  const [password, setPassword] = useState('career1234')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  if (!open) return null

  const submit = async event => {
    event.preventDefault(); setLoading(true); setError('')
    try { await onLogin(email, password); onClose() } catch { setError('이메일 또는 비밀번호를 확인해 주세요.') } finally { setLoading(false) }
  }

  return <div className="modal-backdrop" onMouseDown={onClose}><section className="login-modal" role="dialog" aria-modal="true" aria-labelledby="login-title" onMouseDown={event => event.stopPropagation()}><button className="modal-close" onClick={onClose} aria-label="로그인 닫기">×</button><span className="section-kicker">MEMBER LOGIN</span><h2 id="login-title">커리어픽 로그인</h2><p>자기소개서와 커뮤니티 활동을 이어서 관리하세요.</p><form onSubmit={submit}><label>이메일<input type="email" value={email} onChange={event => setEmail(event.target.value)} required/></label><label>비밀번호<input type="password" value={password} onChange={event => setPassword(event.target.value)} minLength="8" required/></label>{error && <p className="form-error">{error}</p>}<button className="apply-button" disabled={loading}>{loading ? '로그인 중…' : '로그인'}</button></form><small>데모 계정: demo@careerpick.kr / career1234</small></section></div>
}
