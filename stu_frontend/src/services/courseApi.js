export async function createCourse({ teacherId, courseName, className, file }) {
  const formData = new FormData()
  formData.append('teacherId', teacherId)
  formData.append('courseName', courseName)
  formData.append('className', className)
  formData.append('file', file)

  const res = await fetch('/api/courses/create', {
    method: 'POST',
    body: formData,
  })
  const data = await res.json().catch(() => ({}))
  if (!res.ok || !data.success) {
    throw new Error(data.message || '新建课程失败，请稍后重试')
  }
  return data
}

export async function getTeacherCourses(teacherId) {
  const query = new URLSearchParams({ teacherId }).toString()
  const res = await fetch(`/api/courses?${query}`)
  const data = await res.json().catch(() => ({}))
  if (!res.ok || !data.success) {
    throw new Error(data.message || '查询课程失败，请稍后重试')
  }
  return data.data || []
}

export async function getProjectsByCourse(courseId) {
  const query = new URLSearchParams({ courseId }).toString()
  const res = await fetch(`/api/projects?${query}`)
  const data = await res.json().catch(() => ({}))
  if (!res.ok || !data.success) {
    throw new Error(data.message || '查询项目失败，请稍后重试')
  }
  return data.data || []
}

export async function updateProjectStatus({ courseId, projectId, status, deadlineAt }) {
  const formData = new FormData()
  formData.append('courseId', courseId)
  formData.append('projectId', projectId)
  formData.append('status', status)
  if (deadlineAt) {
    formData.append('deadlineAt', deadlineAt)
  }
  const res = await fetch('/api/projects/status', {
    method: 'POST',
    body: formData,
  })
  const data = await res.json().catch(() => ({}))
  if (!res.ok || !data.success) {
    throw new Error(data.message || '更新项目状态失败')
  }
  return data
}

export async function notifyProjectStudents({ courseId, projectId }) {
  const formData = new FormData()
  formData.append('courseId', courseId)
  formData.append('projectId', projectId)
  const res = await fetch('/api/projects/notify', {
    method: 'POST',
    body: formData,
  })
  const data = await res.json().catch(() => ({}))
  if (!res.ok || !data.success) {
    throw new Error(data.message || '发送通知失败')
  }
  return data
}
