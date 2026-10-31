import {useEffect, useState} from 'react'
import {getCoverLetter} from '../shared/api/careerpickApi.js'

const savedAtFormat = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul', dateStyle: 'medium', timeStyle: 'short'
})

export function CoverLetterDetailContent({view, onRetry, onRequireLogin}) {
  if (view.status === 'signed-out') {
    return <div className="cover-list-state">
      <p>로그인하면 자기소개서를 확인할 수 있습니다.</p>
      <button className="login-button" onClick={onRequireLogin}>로그인</button>
    </div>
  }
  if (view.status === 'checking' || view.status === 'loading') {
    return <p role="status">
      {view.status === 'checking' ? '로그인 정보를 확인하고 있습니다.' : '자기소개서를 불러오고 있습니다.'}
    </p>
  }
  if (view.status === 'not-found') {
    return <p role="alert">자기소개서를 찾을 수 없습니다. 목록에서 다시 선택해 주세요.</p>
  }
  if (view.status === 'error') {
    return <div className="cover-list-state">
      <p role="alert">
        {view.error.message}
      </p>
      <button className="login-button" onClick={onRetry}>다시 시도</button>
    </div>
  }
  const detail = view.detail
  return <>
    <p className="cover-detail-job">
      {`${detail.company} · ${detail.jobTitle}`}
    </p>
    <p className="cover-detail-saved">
      {'마지막 저장 '}
      <time dateTime={detail.updatedAt}>
        {savedAtFormat.format(new Date(detail.updatedAt))}
      </time>
    </p>
    <section className="cover-editor" aria-label="자기소개서 문항과 저장된 답변">
      <div className="cover-progress">
        <strong>자기소개서</strong>
        <span>
          {`문항 ${detail.items.length}개`}
        </span>
      </div>
      {detail.items.map((item, index) => <label key={item.id} htmlFor={`cover-item-${item.id}`}>
        <b>
          {`${index + 1}. ${item.questionText}`}
        </b>
        <span id={`cover-item-${item.id}-count`}>
          {`${item.answer.length} / ${item.maxLength}`}
        </span>
        <textarea
          id={`cover-item-${item.id}`}
          value={item.answer}
          maxLength={item.maxLength}
          readOnly
          aria-describedby={`cover-item-${item.id}-count`}
        />
      </label>)}
    </section>
  </>
}

export default function CoverLetterPage({coverLetterId, token, user, sessionError, onRetrySession,
  onSessionExpired, onRequireLogin, onBack}) {
  const [result, setResult] = useState(null)
  const [attempt, setAttempt] = useState(0)
  const userId = user?.id

  useEffect(() => {
    if (!coverLetterId || !token || !userId) return
    const controller = new AbortController()
    getCoverLetter(coverLetterId, token, {signal: controller.signal})
      .then(detail => {
        if (!controller.signal.aborted) setResult({coverLetterId, token, userId, attempt, status: 'ready', detail})
      })
      .catch(error => {
        if (controller.signal.aborted) return
        if (error.status === 401) onSessionExpired(token)
        else setResult({coverLetterId, token, userId, attempt, status: error.status === 404 ? 'not-found' : 'error', error})
      })
    return () => controller.abort()
  }, [coverLetterId, token, userId, attempt, onSessionExpired])

  let view
  if (!token) view = {status: 'signed-out'}
  else if (!userId && sessionError) view = {status: 'error', error: sessionError}
  else if (!userId) view = {status: 'checking'}
  else if (!coverLetterId) view = {status: 'not-found'}
  else if (result?.coverLetterId !== coverLetterId || result?.token !== token || result?.userId !== userId || result?.attempt !== attempt) view = {status: 'loading'}
  else view = result

  return (
    <main className="cover-page" data-cover-letter-id={coverLetterId}>
      <div className="cover-head">
        <button onClick={onBack}>← 목록으로</button>
        <span className="section-kicker">COVER LETTER</span>
        <h1>자기소개서</h1>
      </div>
      <div className="cover-detail-content" aria-busy={view.status === 'checking' || view.status === 'loading'}>
        <CoverLetterDetailContent
          view={view}
          onRequireLogin={onRequireLogin}
          onRetry={!userId && sessionError ? onRetrySession : () => setAttempt(current => current + 1)}
        />
      </div>
    </main>
  )
}
