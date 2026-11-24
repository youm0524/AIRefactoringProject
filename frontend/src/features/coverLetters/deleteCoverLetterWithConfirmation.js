import { deleteCoverLetter } from '../../shared/api/careerpickApi.js'

export const deleteCoverLetterWithConfirmation = async (letter, token, {
  confirm = message => window.confirm(message), signal
} = {}) => {
  if (!confirm(`${letter.company} · ${letter.jobTitle} 자기소개서를 삭제할까요? 문항과 답변도 함께 삭제됩니다.`)) return 'cancelled'
  try {
    await deleteCoverLetter(letter.id, token, {signal})
    return 'deleted'
  } catch (error) {
    if (error.status === 404) return 'missing'
    throw error
  }
}
