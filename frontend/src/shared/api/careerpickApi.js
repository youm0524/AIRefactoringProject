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
const withAuth = token => ({Authorization: `Bearer ${token}`})

export const getJobs = (query = '', category = '전체') =>
  fetch(`${API_URL}/jobs?query=${encodeURIComponent(query)}&category=${encodeURIComponent(category)}`).then(parseJson)

export const login = (email, password) => fetch(`${API_URL}/auth/login`, {
  method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify({email, password})
}).then(parseJson)

export const getMe = token => fetch(`${API_URL}/auth/me`, {headers: withAuth(token)}).then(parseJson)

export const joinCommunity = (jobId, token) => fetch(`${API_URL}/jobs/${jobId}/community-membership`, {
  method: 'PUT', headers: withAuth(token)
}).then(parseJson)

export const getCommunityRooms = token => fetch(`${API_URL}/community-rooms`, {
  headers: withAuth(token)
}).then(parseJson)

export const getCommunityMessages = (roomId, token) => fetch(`${API_URL}/community-rooms/${roomId}/messages`, {
  headers: withAuth(token)
}).then(parseJson)

export const leaveCommunity = (roomId, token) => fetch(`${API_URL}/community-rooms/${roomId}/membership`, {
  method: 'DELETE', headers: withAuth(token)
}).then(parseJson)
export const login = (email, password, {signal} = {}) => fetch(`${API_URL}/auth/login`, {
  method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify({email, password}), signal
}).then(parseJson)

export const getMe = (token, {signal} = {}) => fetch(`${API_URL}/auth/me`, {headers:{Authorization:`Bearer ${token}`}, signal}).then(parseJson)

export const getCoverLetters = (token, page = 0, size = 20, {signal} = {}) =>
  fetch(`${API_URL}/cover-letters?page=${encodeURIComponent(page)}&size=${encodeURIComponent(size)}`, {
    headers: {Authorization: `Bearer ${token}`}, signal
  }).then(parseJson)

export const createCoverLetter = (jobId, token, {signal} = {}) =>
  fetch(`${API_URL}/cover-letters`, {
    method: 'POST', headers: {'Content-Type': 'application/json', Authorization: `Bearer ${token}`},
    body: JSON.stringify({jobId}), signal
  }).then(parseJson)

export const getCoverLetter = (coverLetterId, token, {signal} = {}) =>
  fetch(`${API_URL}/cover-letters/${encodeURIComponent(coverLetterId)}`, {
    headers: {Authorization: `Bearer ${token}`}, signal
  }).then(parseJson)

export const updateCoverLetter = (coverLetterId, items, token, {signal} = {}) =>
  fetch(`${API_URL}/cover-letters/${encodeURIComponent(coverLetterId)}`, {
    method: 'PUT', headers: {'Content-Type': 'application/json', Authorization: `Bearer ${token}`},
    body: JSON.stringify({items}), signal
  }).then(parseJson)

export const deleteCoverLetter = (coverLetterId, token, {signal} = {}) =>
  fetch(`${API_URL}/cover-letters/${encodeURIComponent(coverLetterId)}`, {
    method: 'DELETE', headers: {Authorization: `Bearer ${token}`}, signal
  }).then(parseJson)
