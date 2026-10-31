import test from 'node:test'
import assert from 'node:assert/strict'
import {getCoverLetterAnswers, buildCoverLetterAnswerItems} from '../src/features/coverLetters/answers.js'

const detail = {id: 17, items: [
  {id: 101, questionText: '첫 문항', answer: '저장한 답변', maxLength: 700},
  {id: 102, questionText: '다음 문항', answer: '', maxLength: 100}
]}

test('초안·문항 ID를 구분하고 전체 답변을 순서대로 요청 형식으로 만든다', () => {
  const answers = getCoverLetterAnswers(detail)
  assert.deepEqual(answers, {101: '저장한 답변', 102: ''})
  answers[101] = '  편집한 답변\n'
  assert.deepEqual(buildCoverLetterAnswerItems(detail, answers), [
    {id: 101, answer: '  편집한 답변\n'}, {id: 102, answer: ''}
  ])
  assert.equal(detail.items[0].answer, '저장한 답변')
})

test('700자·빈 답변은 허용하고 길이 초과·NULL·문항 누락은 거절한다', () => {
  assert.equal(buildCoverLetterAnswerItems(detail, {101: '가'.repeat(700), 102: ''})[0].answer.length, 700)
  for (const answers of [{101: '가'.repeat(701), 102: ''}, {101: '', 102: null}, {101: ''}]) {
    assert.throws(() => buildCoverLetterAnswerItems(detail, answers), /번 문항은/)
  }
})

test('이모지는 UTF-16 길이로 제한하고 검증 실패 시 원본 입력을 변경하지 않는다', () => {
  const answers = {101: '😀'.repeat(350), 102: ''}
  assert.equal(buildCoverLetterAnswerItems(detail, answers)[0].answer.length, 700)
  answers[101] += '😀'
  const before = {...answers}
  assert.throws(() => buildCoverLetterAnswerItems(detail, answers), /700자/)
  assert.deepEqual(answers, before)
  assert.equal(detail.items[0].answer, '저장한 답변')
})
