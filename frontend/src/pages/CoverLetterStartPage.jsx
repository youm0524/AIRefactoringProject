import {useEffect, useState} from 'react'
import {createCoverLetter} from '../shared/api/careerpickApi.js'

export default function CoverLetterStartPage({job, token, user, sessionError, onRetrySession,
  onRequireLogin, onSessionExpired, onCreated, onBack}) {
  const [failure, setFailure] = useState(null)
  const [attempt, setAttempt] = useState(0)
  const userId = user?.id
  const jobId = job.id

  useEffect(() => {
    if (!token || !userId) return
    const controller = new AbortController()
    createCoverLetter(jobId, token, {signal: controller.signal})
      .then(detail => {
        if (!controller.signal.aborted) onCreated(detail, jobId, token)
      })
      .catch(error => {
        if (controller.signal.aborted) return
        if (error.status === 401) onSessionExpired(token)
        else setFailure({jobId, token, userId, attempt, error})
      })
    return () => controller.abort()
  }, [jobId, token, userId, attempt, onCreated, onSessionExpired])

  const error = !userId && sessionError ? sessionError
    : failure?.jobId === jobId && failure?.token === token && failure?.userId === userId && failure?.attempt === attempt
      ? failure.error : null

  return (
    <main className="cover-page">
      <div className="cover-head">
        <button onClick={onBack}>← 목록으로</button>
        <span className="section-kicker">COVER LETTER</span>
        <h1>자기소개서 작성</h1>
        <p>
          {`${job.company} · ${job.title}`}
        </p>
      </div>
      <section className="cover-list-state">
        {!token ? <>
          <p>로그인하면 선택한 공고의 자기소개서를 작성할 수 있습니다.</p>
          <button className="login-button" onClick={onRequireLogin}>로그인</button>
        </> : error ? <>
          <p role="alert">
            {error.status === 404 ? '공고를 찾을 수 없습니다. 채용공고를 다시 확인해 주세요.' : error.message}
          </p>
          <button className="login-button" onClick={!userId ? onRetrySession : () => setAttempt(current => current + 1)}>다시 시도</button>
        </> : <p role="status">
          {!userId ? '로그인 정보를 확인하고 있습니다.' : '자기소개서를 준비하고 있습니다.'}
        </p>}
      </section>
    </main>
  )
}
