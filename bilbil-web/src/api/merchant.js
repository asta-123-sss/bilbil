import request from './request'

export const applyMerchant = (data) => request.post('/merchant/apply', data)
export const myApply = () => request.get('/merchant/apply')
