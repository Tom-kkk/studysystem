export async function getTeacherReportList({ teacherId, courseId, projectId }) {
  const query = new URLSearchParams({ teacherId, courseId, projectId }).toString()
  const res = await fetch(`/api/teacher/reports/list?${query}`)
  const data = await res.json().catch(() => ({}))
  if (!res.ok || !data.success) {
    throw new Error(data.message || '查询报告上传情况失败')
  }
  return data.data || []
}

export function getTeacherReportFileUrl({ teacherId, courseId, projectId, sno }) {
  const query = new URLSearchParams({ teacherId, courseId, projectId, sno }).toString()
  return `/api/teacher/reports/file?${query}`
}

export function getTeacherReportExportUrl({ teacherId, courseId, projectId }) {
  const query = new URLSearchParams({ teacherId, courseId, projectId }).toString()
  return `/api/teacher/reports/export?${query}`
}

export async function updateTeacherReportGrade({ teacherId, courseId, sno, grade }) {
  const formData = new FormData()
  formData.append('teacherId', teacherId)
  formData.append('courseId', courseId)
  formData.append('sno', sno)
  if (grade !== undefined && grade !== null) {
    formData.append('grade', grade)
  }
  const res = await fetch('/api/teacher/reports/grade', {
    method: 'POST',
    body: formData,
  })
  const data = await res.json().catch(() => ({}))
  if (!res.ok || !data.success) {
    throw new Error(data.message || '保存成绩失败')
  }
  return data
}
