import request from './request'

export const createAd = (data) => request.post('/ad', data)
export const myAds = (params) => request.get('/ad/my', { params })
export const updateAdStatus = (id, status) => request.put(`/ad/${id}/status`, { status })
export const deleteAd = (id) => request.delete(`/ad/${id}`)
export const listAdFeed = () => request.get('/ad/feed')
export const clickAd = (id) => request.post(`/ad/${id}/click`)

export const uploadAdCover = (file) => {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/ad/cover', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
}
