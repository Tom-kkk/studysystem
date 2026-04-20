export async function getStudentReportList(sno) {
  const query = new URLSearchParams({ sno }).toString()
  const res = await fetch(`/api/student/reports/list?${query}`)
  const data = await res.json().catch(() => ({}))
  if (!res.ok || !data.success) {
    throw new Error(data.message || '查询学生报告列表失败')
  }
  return data.data || []
}

export function getStudentReportFileUrl({ sno, courseId, projectId }) {
  const query = new URLSearchParams({ sno, courseId, projectId }).toString()
  return `/api/student/reports/file?${query}`
}

export function uploadStudentReport({ sno, courseId, projectId, file, onProgress }) {
  return new Promise((resolve, reject) => {
    if (!file) {
      reject(new Error('请选择要上传的 PDF 文件'))
      return
    }
    const fileName = (file.name || '').toLowerCase()
    const isPdf = file.type === 'application/pdf' || fileName.endsWith('.pdf')
    if (!isPdf) {
      reject(new Error('文件类型错误，仅支持 PDF'))
      return
    }
    const maxSize = 8 * 1024 * 1024
    if (file.size <= 0 || file.size > maxSize) {
      reject(new Error('文件大小需在 0~8MB'))
      return
    }

    const formData = new FormData()
    formData.append('sno', sno)
    formData.append('courseId', courseId)
    formData.append('projectId', projectId)
    formData.append('file', file)

    const xhr = new XMLHttpRequest()
    xhr.open('POST', '/api/student/reports/upload')
    xhr.onload = () => {
      let data = {}
      try {
        data = JSON.parse(xhr.responseText || '{}')
      } catch (e) {
        // ignore
      }
      if (xhr.status >= 200 && xhr.status < 300 && data.success) {
        resolve(data)
      } else {
        let fallbackMsg = ''
        if (typeof xhr.responseText === 'string' && xhr.responseText.trim()) {
          fallbackMsg = xhr.responseText.trim()
        }
        reject(new Error(data.message || fallbackMsg || `上传失败（HTTP ${xhr.status}）`))
      }
    }
    xhr.onerror = () => reject(new Error('网络错误，上传失败'))
    xhr.upload.onprogress = (event) => {
      if (!event.lengthComputable || typeof onProgress !== 'function') return
      const percent = Math.round((event.loaded / event.total) * 100)
      onProgress(percent)
    }
    xhr.send(formData)
  })
}

