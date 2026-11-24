export const getCoverLetterAnswers = detail =>
  Object.fromEntries(detail.items.map(item => [item.id, item.answer]))

export const hasUnsavedCoverLetterAnswers = (detail, answers) =>
  Boolean(detail && answers && detail.items.some(item => answers[item.id] !== item.answer))

export const buildCoverLetterAnswerItems = (detail, answers) => detail.items.map((item, index) => {
  const answer = answers?.[item.id]
  if (typeof answer !== 'string' || answer.length > item.maxLength) {
    throw new Error(`${index + 1}번 문항은 ${item.maxLength}자 이내로 작성해 주세요.`)
  }
  return {id: item.id, answer}
})
