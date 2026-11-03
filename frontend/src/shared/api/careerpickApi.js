const parseJson = async response => {
  if (!response.ok) {
    const error = new Error(`API 오류: ${response.status}`)
    error.status = response.status
    throw error
  }
  if (response.status === 204) return null
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
