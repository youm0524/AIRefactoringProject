import {getCoverLetterAnswers} from './answers.js'

export function restoreCoverLetterAnswers(detail, userId, recovery) {
  const answers = getCoverLetterAnswers(detail)
  if (recovery?.userId !== userId || recovery?.coverLetterId !== detail.id) return answers
  for (const item of detail.items) {
    const answer = recovery.answers[item.id]
    if (typeof answer === 'string') answers[item.id] = answer
  }
  return answers
}
