import request from '@/utils/request'

export function checkFileExists(md5) {
  return request({
    url: '/file/check-exists',
    method: 'get',
    params: { md5 }
  })
}

export function uploadFile(formData, config = {}) {
  return request({
    url: '/file/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    ...config
  })
}

export function fetchFileList(versionId) {
  return request({
    url: `/file/list/${versionId}`,
    method: 'get'
  })
}

export function deleteFile(fileId) {
  return request({
    url: `/file/${fileId}`,
    method: 'delete'
  })
}

export function getPreviewUrl(fileId) {
  return request({
    url: `/file/preview/${fileId}`,
    method: 'get'
  })
}

export function downloadFile(fileId) {
  return request({
    url: `/file/download/${fileId}`,
    method: 'get',
    responseType: 'blob'
  })
}
