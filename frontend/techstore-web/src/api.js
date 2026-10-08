import axios from 'axios'

// En local apunta a :8080. En AWS (.env.production) queda vacío = mismo dominio.
export const BACKEND = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

const api = axios.create({ baseURL: `${BACKEND}/api` })

api.interceptors.request.use((cfg) => {
  const token = localStorage.getItem('token')
  if (token) cfg.headers.Authorization = `Bearer ${token}`
  return cfg
})

export default api