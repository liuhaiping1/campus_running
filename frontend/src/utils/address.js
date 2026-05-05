/**
 * 将地址对象转为选址组件使用的 { name, lng, lat } 格式
 */
export function addressToLocation(addr) {
  return {
    name: [addr.campusName, addr.buildingName, addr.detailAddress].filter(Boolean).join(' '),
    lng: Number(addr.longitude),
    lat: Number(addr.latitude)
  }
}
