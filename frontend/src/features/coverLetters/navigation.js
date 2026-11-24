import {hasUnsavedCoverLetterAnswers} from './answers.js'

export const canLeaveCoverLetter = ({detail, answers, isSaving}, confirm = message => window.confirm(message)) => {
  if (isSaving) return false
  if (!hasUnsavedCoverLetterAnswers(detail, answers)) return true
  return confirm('저장하지 않은 변경사항이 있습니다. 변경사항을 버리고 이동할까요?')
}
