import request from './request'

export const listConversations = () => request.get('/pm/conversations')
export const chatWith = (otherUserId) => request.get(`/pm/chat/${otherUserId}`)
export const sendPm = (data) => request.post('/pm/send', data)
export const pmUnreadCount = () => request.get('/pm/unread-count')
export const markPmRead = (otherUserId) => request.put(`/pm/read/${otherUserId}`)
