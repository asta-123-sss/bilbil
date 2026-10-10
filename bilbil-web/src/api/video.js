import request from './request'

// ---- 分块上传 ----
export const initUpload = (data) => request.post('/video/upload/init', data)

export const uploadChunk = (uploadId, chunkIndex, chunkMd5, file) => {
  const fd = new FormData()
  fd.append('uploadId', uploadId)
  fd.append('chunkIndex', chunkIndex)
  if (chunkMd5) fd.append('chunkMd5', chunkMd5)
  fd.append('file', file)
  return request.post('/video/upload/chunk', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
}

export const mergeUpload = (data) => request.post('/video/upload/merge', data)

export const uploadCover = (file) => {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/video/cover', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
}

// ---- 视频信息 ----
export const listVideos = (params) => request.get('/video/list', { params })
export const listRecommend = (params) => request.get('/video/recommend', { params })
export const getVideoDetail = (id) => request.get(`/video/detail/${id}`)
export const playVideo = (id) => request.get(`/video/play/${id}`)
export const myVideos = (params) => request.get('/video/my', { params })
export const updateVideo = (id, data) => request.put(`/video/${id}`, data)
export const deleteVideo = (id) => request.delete(`/video/${id}`)

// ---- 互动 ----
export const like = (id) => request.post(`/video/${id}/like`)
export const unlike = (id) => request.delete(`/video/${id}/like`)
export const collect = (id) => request.post(`/video/${id}/collect`)
export const uncollect = (id) => request.delete(`/video/${id}/collect`)
export const coin = (id) => request.post(`/video/${id}/coin`)
export const uncoin = (id) => request.delete(`/video/${id}/coin`)
