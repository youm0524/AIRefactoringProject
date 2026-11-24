import test from 'node:test'
import assert from 'node:assert/strict'
import {restoreCoverLetterAnswers} from '../src/features/coverLetters/sessionRecovery.js'
import {hasUnsavedCoverLetterAnswers, buildCoverLetterAnswerItems} from '../src/features/coverLetters/answers.js'

const detail = {id: 17, items: [{id: 101, answer: '서버 답변', maxLength: 700}, {id: 102, answer: '기존', maxLength: 700}]}
const recovery = {userId: 1, coverLetterId: 17, answers: {101: '  편집\n😀', 102: ''}}

test('같은 사용자·초안만 공백과 빈 답변까지 복구하고 저장 전 변경으로 유지한다', () => {
  const answers = restoreCoverLetterAnswers(detail, 1, recovery)
  assert.deepEqual(answers, recovery.answers)
  assert.equal(hasUnsavedCoverLetterAnswers(detail, answers), true)
  assert.deepEqual(buildCoverLetterAnswerItems(detail, answers), [{id: 101, answer: '  편집\n😀'}, {id: 102, answer: ''}])
  assert.equal(detail.items[0].answer, '서버 답변')
})

test('다른 사용자·초안과 제거된 복구값은 서버 답변만 표시한다', () => {
  for (const [userId, snapshot] of [[2, recovery], [1, {...recovery, coverLetterId: 18}], [1, null]]) {
    assert.deepEqual(restoreCoverLetterAnswers(detail, userId, snapshot), {101: '서버 답변', 102: '기존'})
  }
})

test('재조회 문항 ID로 복구하고 길이 제한이 바뀌어도 입력을 잘라내지 않는다', () => {
  const changed = {id: 17, items: [{id: 101, answer: '', maxLength: 1}, {id: 103, answer: '새 문항', maxLength: 700}]}
  const answers = restoreCoverLetterAnswers(changed, 1, recovery)
  assert.deepEqual(answers, {101: '  편집\n😀', 103: '새 문항'})
  assert.throws(() => buildCoverLetterAnswerItems(changed, answers), /1자/)
})
