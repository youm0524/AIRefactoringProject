const parseJson = async response => {
  if (!response.ok) {
    const body = await response.json().catch(() => null)
    const error = new Error(body?.message || `API 오류: ${response.status}`)
    error.status = response.status
    error.code = body?.code || null
    throw error
  }
  if (response.status === 204) return undefined
  return response.json()
}

const API_URL = 'http://localhost:8080/api'

export const getJobs = (query = '', category = '전체') =>
  fetch(`${API_URL}/jobs?query=${encodeURIComponent(query)}&category=${encodeURIComponent(category)}`).then(parseJson)

export const getMessages = jobId => fetch(`${API_URL}/jobs/${jobId}/messages`).then(parseJson)

export const sendMessage = (jobId, text, token) => fetch(`${API_URL}/jobs/${jobId}/messages`, {
  method: 'POST', headers: {'Content-Type':'application/json', Authorization:`Bearer ${token}`}, body: JSON.stringify({text})
}).then(parseJson)

export const login = (email, password) => fetch(`${API_URL}/auth/login`, {
  method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify({email, password})
}).then(parseJson)

export const getMe = token => fetch(`${API_URL}/auth/me`, {headers:{Authorization:`Bearer ${token}`}}).then(parseJson)

export const getCoverLetters = (token, page = 0, size = 20, {signal} = {}) =>
  fetch(`${API_URL}/cover-letters?page=${encodeURIComponent(page)}&size=${encodeURIComponent(size)}`, {
    headers: {Authorization: `Bearer ${token}`}, signal
  }).then(parseJson)

export const deleteCoverLetter = (coverLetterId, token, {signal} = {}) =>
  fetch(`${API_URL}/cover-letters/${encodeURIComponent(coverLetterId)}`, {
    method: 'DELETE', headers: {Authorization: `Bearer ${token}`}, signal
  }).then(parseJson)
