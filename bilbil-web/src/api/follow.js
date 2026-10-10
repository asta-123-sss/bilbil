import request from './request'

export const follow = (id) => request.post(`/follow/${id}`)
export const unfollow = (id) => request.delete(`/follow/${id}`)
export const followings = (id, params) => request.get(`/follow/${id}/followings`, { params })
export const fans = (id, params) => request.get(`/follow/${id}/fans`, { params })
