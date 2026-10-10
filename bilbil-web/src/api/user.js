import request from './request'

export const register = (data) => request.post('/user/register', data)
export const login = (data) => request.post('/user/login', data)
export const logout = () => request.post('/user/logout')
export const getMe = () => request.get('/user/me')
export const updateInfo = (data) => request.put('/user/info', data)
export const changePassword = (data) => request.put('/user/password', data)
export const getUserProfile = (id) => request.get(`/user/profile/${id}`)

export const uploadAvatar = (file) => {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/user/avatar', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
}
