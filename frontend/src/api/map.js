import request from '@/utils/request'

export function estimateRoute(data) {
  return request.post('/api/map/route/estimate', data)
}

export function getCampusLocations(params) {
  return request.get('/api/campus-location/list', { params })
}
