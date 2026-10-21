const parseJson = async response => {
  if (!response.ok) throw new Error(`API 오류: ${response.status}`)
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
