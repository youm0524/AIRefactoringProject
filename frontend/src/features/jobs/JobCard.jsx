export default function JobCard({job, saved, onOpen, onSave}) {
  const keyOpen = event => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      onOpen(job)
    }
  }

  return <article className="job-card" role="button" tabIndex="0" onClick={() => onOpen(job)} onKeyDown={keyOpen} aria-label={`${job.title} 상세보기`}><div className="job-top"><span className="company-logo" style={{background:job.color}}>{job.logo}</span><p>{job.company}<small>{job.location} · {job.type}</small></p><button className={`save-button ${saved ? 'saved' : ''}`} onClick={event => {event.stopPropagation(); onSave(job.id)}} aria-label={`${job.company} 공고 저장`}>{saved ? '♥' : '♡'}</button></div><h3>{job.title}</h3><div className="tags">{job.tags.map(tag => <span key={tag}>{tag}</span>)}</div><div className="deadline"><span>{job.type} · {job.location}</span><strong>{job.deadline}</strong></div></article>
}
