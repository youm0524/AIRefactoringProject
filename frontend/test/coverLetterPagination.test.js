import test from 'node:test'
import assert from 'node:assert/strict'
import {getLastCoverLetterPage} from '../src/features/coverLetters/pagination.js'

test('빈 목록과 페이지 크기의 경계에서 마지막 페이지를 계산한다', () => {
  for (const [total, expected] of [[0, 0], [1, 0], [20, 0], [21, 1], [40, 1], [41, 2]]) {
    assert.equal(getLastCoverLetterPage(total, 20), expected)
  }
})

test('마지막 항목 삭제와 여러 페이지 감소 시 유효한 페이지로 보정한다', () => {
  const currentPage = 2
  assert.equal(Math.min(currentPage, getLastCoverLetterPage(40, 20)), 1)
  assert.equal(Math.min(currentPage, getLastCoverLetterPage(20, 20)), 0)
  assert.equal(Math.min(currentPage, getLastCoverLetterPage(0, 20)), 0)
  assert.equal(getLastCoverLetterPage(6, 5), 1)
})
