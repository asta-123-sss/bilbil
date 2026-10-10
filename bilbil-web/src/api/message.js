import request from './request'

export const listMessages = (params) => request.get('/message/list', { params })
export const unreadCount = () => request.get('/message/unread-count')
export const markRead = (id) => request.put(`/message/read/${id}`)
export const markAllRead = () => request.put('/message/read-all')
