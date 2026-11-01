import test from 'node:test'
import assert from 'node:assert/strict'
import {getCoverLetterAnswers, hasUnsavedCoverLetterAnswers} from '../src/features/coverLetters/answers.js'
import {canLeaveCoverLetter} from '../src/features/coverLetters/navigation.js'

const detail = {id: 17, items: [{id: 101, answer: '저장된 답변'}, {id: 102, answer: ''}]}
const noConfirm = () => assert.fail('확인 창이 필요하지 않은 이동')

test('조회 중이거나 저장값과 같으면 확인 없이 이동한다', () => {
  assert.equal(canLeaveCoverLetter({detail: null, answers: null}, noConfirm), true)
  assert.equal(canLeaveCoverLetter({detail, answers: getCoverLetterAnswers(detail)}, noConfirm), true)
})

test('미저장 변경의 이탈 취소는 화면 이동을 거절하고 입력을 보존한다', () => {
  const answers = {101: '편집 중인 답변', 102: ''}
  let asked = 0
  assert.equal(canLeaveCoverLetter({detail, answers}, message => {
    asked++
    assert.match(message, /변경사항을 버리고 이동/)
    return false
  }), false)
  assert.equal(asked, 1)
  assert.equal(answers[101], '편집 중인 답변')
  assert.equal(detail.items[0].answer, '저장된 답변')
})

test('미저장 변경 폐기를 확인하면 이동을 허용한다', () => {
  assert.equal(canLeaveCoverLetter({detail, answers: {101: '', 102: '다른 답변'}}, () => true), true)
})

test('원래 값으로 되돌리면 미저장이 아니며 공백·줄바꿈은 변경으로 판정한다', () => {
  const answers = getCoverLetterAnswers(detail)
  answers[101] = '임시 편집'
  assert.equal(hasUnsavedCoverLetterAnswers(detail, answers), true)
  answers[101] = '저장된 답변'
  assert.equal(canLeaveCoverLetter({detail, answers}, noConfirm), true)
  for (const value of ['저장된 답변 ', '저장된 답변\n', '']) {
    assert.equal(hasUnsavedCoverLetterAnswers(detail, {...answers, 101: value}), true)
  }
})

test('저장 중에는 변경 여부와 관계없이 이동·폐기 확인을 막는다', () => {
  assert.equal(canLeaveCoverLetter({detail, answers: getCoverLetterAnswers(detail), isSaving: true}, noConfirm), false)
  assert.equal(canLeaveCoverLetter({detail, answers: {101: '편집', 102: ''}, isSaving: true}, noConfirm), false)
})

test('저장 성공 응답은 비교 기준을 갱신하고 실패 시에는 이전 기준을 유지한다', () => {
  const answers = {101: '새 답변', 102: ''}
  assert.equal(hasUnsavedCoverLetterAnswers(detail, answers), true)
  const saved = {id: 17, items: [{id: 101, answer: '새 답변'}, {id: 102, answer: ''}]}
  assert.equal(canLeaveCoverLetter({detail: saved, answers: getCoverLetterAnswers(saved)}, noConfirm), true)
  assert.equal(canLeaveCoverLetter({detail, answers}, () => false), false)
})
