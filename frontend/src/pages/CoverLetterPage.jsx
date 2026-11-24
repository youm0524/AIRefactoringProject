import {useEffect, useLayoutEffect, useRef, useState} from 'react'
import {getCoverLetter, updateCoverLetter} from '../shared/api/careerpickApi.js'
import {getCoverLetterAnswers, buildCoverLetterAnswerItems, hasUnsavedCoverLetterAnswers} from '../features/coverLetters/answers.js'
import {restoreCoverLetterAnswers} from '../features/coverLetters/sessionRecovery.js'
import {canLeaveCoverLetter} from '../features/coverLetters/navigation.js'

const savedAtFormat = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul', dateStyle: 'medium', timeStyle: 'short'
})

export function CoverLetterDetailContent({view, onRetry, onRequireLogin, answers, onAnswerChange, onSave, saveState}) {
  const isSaving = saveState?.status === 'saving'
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
    <form className="cover-editor" aria-label="자기소개서 문항과 답변" onSubmit={event => {
      event.preventDefault()
      if (!isSaving) onSave?.()
    }}>
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
          {`${(answers?.[item.id] ?? item.answer).length} / ${item.maxLength}`}
        </span>
        <textarea
          id={`cover-item-${item.id}`}
          value={answers?.[item.id] ?? item.answer}
          maxLength={item.maxLength}
          disabled={isSaving}
          onChange={event => onAnswerChange?.(item.id, event.target.value)}
          placeholder="답변을 작성해 주세요."
          aria-describedby={`cover-item-${item.id}-count`}
        />
      </label>)}
      {saveState?.status === 'success' && <div className="cover-save-success" role="status">
        <span className="cover-save-success-icon" aria-hidden="true">✓</span>
        <span>자기소개서를 저장했습니다.</span>
      </div>}
      {!isSaving && hasUnsavedCoverLetterAnswers(detail, answers) && <div className="cover-unsaved-warning" role="status">
        <span className="cover-unsaved-warning-icon" aria-hidden="true">!</span>
        <span>저장하지 않은 변경사항이 있습니다.</span>
      </div>}
      {saveState?.status === 'error' && <div className="cover-detail-save-error">
        <p role="alert">
          {saveState.error.status === 401 ? '로그인이 만료되었습니다. 다시 로그인해 주세요.' : saveState.error.message}
        </p>
        {saveState.error.status === 401 && <button type="button" className="login-button" onClick={onRequireLogin}>다시 로그인</button>}
      </div>}
      <div className="cover-actions">
        <button type="submit" className="apply-button" disabled={isSaving}>
          {isSaving ? '저장 중…' : saveState?.status === 'error' ? '저장 다시 시도' : '초안 저장'}
        </button>
      </div>
    </form>
  </>
}

export default function CoverLetterPage({coverLetterId, token, user, sessionError, onRetrySession,
  onSessionExpired, onRequireLogin, onBack, onRegisterLeaveGuard, onRegisterSnapshot, recovery}) {
  const [result, setResult] = useState(null)
  const [attempt, setAttempt] = useState(0)
  const [saveState, setSaveState] = useState(null)
  const activeSave = useRef(null)
  const userId = user?.id

  useEffect(() => () => {
    activeSave.current?.controller.abort()
    activeSave.current = null
  }, [coverLetterId, token, userId])

  useEffect(() => {
    if (!coverLetterId || !token || !userId) return
    const controller = new AbortController()
    getCoverLetter(coverLetterId, token, {signal: controller.signal})
      .then(detail => {
        if (!controller.signal.aborted) setResult({coverLetterId, token, userId, attempt, status: 'ready', detail, answers: restoreCoverLetterAnswers(detail, userId, recovery)})
      })
      .catch(error => {
        if (controller.signal.aborted) return
        if (error.status === 401) onSessionExpired(token)
        else setResult({coverLetterId, token, userId, attempt, status: error.status === 404 ? 'not-found' : 'error', error})
      })
    return () => controller.abort()
  }, [coverLetterId, token, userId, attempt, onSessionExpired, recovery])

  let view
  if (!token) view = {status: 'signed-out'}
  else if (!userId && sessionError) view = {status: 'error', error: sessionError}
  else if (!userId) view = {status: 'checking'}
  else if (!coverLetterId) view = {status: 'not-found'}
  else if (result?.coverLetterId !== coverLetterId || result?.token !== token || result?.userId !== userId || result?.attempt !== attempt) view = {status: 'loading'}
  else view = result

  useLayoutEffect(() => onRegisterLeaveGuard?.(() => canLeaveCoverLetter({
    detail: view.status === 'ready' ? view.detail : null,
    answers: view.status === 'ready' ? view.answers : null,
    isSaving: Boolean(activeSave.current)
  })), [onRegisterLeaveGuard, view])

  useLayoutEffect(() => onRegisterSnapshot?.(() => view.status === 'ready'
    ? {coverLetterId, userId, answers: {...view.answers}} : null),
  [onRegisterSnapshot, view, coverLetterId, userId])

  const currentSave = saveState?.coverLetterId === coverLetterId && saveState?.token === token && saveState?.userId === userId ? saveState : null
  const saveAnswers = async () => {
    if (activeSave.current || view.status !== 'ready') return
    const scope = {coverLetterId, token, userId}
    let items
    try {
      items = buildCoverLetterAnswerItems(view.detail, view.answers)
    } catch (error) {
      setSaveState({...scope, status: 'error', error})
      return
    }
    const operation = {controller: new AbortController()}
    activeSave.current = operation
    setSaveState({...scope, status: 'saving'})
    try {
      const detail = await updateCoverLetter(coverLetterId, items, token, {signal: operation.controller.signal})
      if (operation.controller.signal.aborted) return
      setResult(current => current?.coverLetterId === coverLetterId && current?.token === token && current?.userId === userId
        ? {...current, detail, answers: getCoverLetterAnswers(detail)} : current)
      setSaveState({...scope, status: 'success'})
    } catch (error) {
      if (operation.controller.signal.aborted) return
      if (error.status === 401) {
        onSessionExpired(token)
        return
      }
      setSaveState({...scope, status: 'error', error: error.status ? error : new Error('서버에 연결할 수 없습니다. 연결을 확인하고 다시 시도해 주세요.')})
    } finally {
      if (activeSave.current === operation) activeSave.current = null
    }
  }

  return (
    <main className="cover-page" data-cover-letter-id={coverLetterId}>
      <div className="cover-head">
        <button onClick={onBack} disabled={currentSave?.status === 'saving'}>← 목록으로</button>
        <span className="section-kicker">COVER LETTER</span>
        <h1>자기소개서</h1>
      </div>
      <div className="cover-detail-content" aria-busy={view.status === 'checking' || view.status === 'loading' || currentSave?.status === 'saving'}>
        <CoverLetterDetailContent
          view={view}
          onRequireLogin={onRequireLogin}
          onRetry={!userId && sessionError ? onRetrySession : () => setAttempt(current => current + 1)}
          answers={view.answers}
          saveState={currentSave}
          onSave={saveAnswers}
          onAnswerChange={(itemId, answer) => {
            if (activeSave.current || view.status !== 'ready') return
            const item = view.detail.items.find(candidate => candidate.id === itemId)
            if (!item || answer.length > item.maxLength) return
            setResult(current => current?.coverLetterId === coverLetterId && current?.token === token && current?.userId === userId
              ? {...current, answers: {...current.answers, [itemId]: answer}} : current)
            setSaveState(null)
          }}
        />
      </div>
    </main>
  )
}
