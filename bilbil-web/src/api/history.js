import request from './request'

export const listHistory = (params) => request.get('/history', { params })
export const reportProgress = (videoId, progress) => request.post(`/video/${videoId}/watch`, { progress })
export const removeHistory = (videoId) => request.delete(`/history/${videoId}`)
export const clearHistory = () => request.delete('/history')
