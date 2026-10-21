import test from 'node:test'
import assert from 'node:assert/strict'
import { filterSampleJobs, sampleJobs } from '../src/sampleJobs.js'

test('검색어와 분야로 샘플 공고를 필터링한다', () => {
  assert.equal(filterSampleJobs(sampleJobs, '네오').length, 1)
  assert.equal(filterSampleJobs(sampleJobs, '인턴').length, 3)
  assert.ok(filterSampleJobs(sampleJobs, '', '개발').every(job => job.category === '개발'))
})
