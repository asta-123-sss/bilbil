import request from './request'

export const publishComment = (data) => request.post('/comment/publish', data)
export const deleteComment = (id) => request.delete(`/comment/${id}`)
export const listComments = (params) => request.get('/comment/list', { params })
export const listReplies = (params) => request.get('/comment/replies', { params })
