import request from './request'

export const listCategories = () => request.get('/category/list')
