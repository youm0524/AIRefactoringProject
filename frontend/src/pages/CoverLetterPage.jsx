import { useState } from 'react'

export default function CoverLetterPage({job, onBack, onNotice}) {
  const key = `careerpick-cover-letter-${job?.id || 'general'}`
  const [draft, setDraft] = useState(() => JSON.parse(localStorage.getItem(key) || '{"motivation":"","experience":""}'))
  const update = (field, value) => setDraft(current => ({...current, [field]: value}))
  const save = () => {localStorage.setItem(key, JSON.stringify(draft)); onNotice('자기소개서 초안을 저장했습니다')}

  return <main className="cover-page">
    <div className="cover-head">
      <button className="cover-back" onClick={onBack}>
        <svg aria-hidden="true" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="m12 5-7 7 7 7M5 12h14"/></svg>
        <span>자기소개서 목록</span></button>{job ? <div className="cover-job-heading"><p className="cover-job-company">{job.company} </p><p>{job.title}</p></div> : <><h2>일반 자기소개서</h2><p>지원할 공고를 선택하거나 자유롭게 초안을 작성해 보세요.</p></>}</div><section className="cover-editor"><div className="cover-progress"><strong>자기소개서</strong><span>문항 2개</span></div><label><b>1. 지원 동기와 입사 후 이루고 싶은 목표를 작성해 주세요.</b><span>{draft.motivation.length} / 700</span><textarea maxLength="700" value={draft.motivation} onChange={event => update('motivation', event.target.value)} placeholder="기업과 직무에 관심을 갖게 된 계기부터 작성해 보세요."/></label><label><b>2. 직무와 관련해 가장 몰입했던 경험을 작성해 주세요.</b><span>{draft.experience.length} / 700</span><textarea maxLength="700" value={draft.experience} onChange={event => update('experience', event.target.value)} placeholder="상황, 행동, 결과가 드러나도록 작성해 보세요."/></label><div className="cover-actions"><button className="modal-save" onClick={onBack}>나가기</button><button className="apply-button" onClick={save}>초안 저장</button></div></section></main>
}
