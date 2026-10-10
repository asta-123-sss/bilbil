import request from './request'

export const sendDanmaku = (data) => request.post('/danmaku/send', data)
export const listDanmaku = (params) => request.get('/danmaku/list', { params })
