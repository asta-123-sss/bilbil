import request from './request'

export const adminVideos = (params) => request.get('/admin/videos', { params })
export const reviewVideo = (id, status) => request.put(`/admin/video/${id}/status`, { status })
export const adminUsers = (params) => request.get('/admin/users', { params })
export const setUserStatus = (id, status) => request.put(`/admin/user/${id}/status`, { status })
export const adminComments = (params) => request.get('/admin/comments', { params })
export const deleteComment = (id) => request.delete(`/admin/comment/${id}`)
export const adminMerchantApplies = (params) => request.get('/admin/merchant-applies', { params })
export const reviewMerchantApply = (id, data) => request.put(`/admin/merchant-apply/${id}/review`, data)
export const adminMerchants = (params) => request.get('/admin/merchants', { params })
export const rechargeMerchant = (id, amount) => request.put(`/admin/merchant/${id}/recharge`, { amount })
export const adminAds = (params) => request.get('/admin/ads', { params })
export const adminAdStatus = (id, status) => request.put(`/admin/ad/${id}/status`, { status })
