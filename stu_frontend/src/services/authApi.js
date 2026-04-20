export async function login({ userName, password }) {
  const res = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userName, password }),
  })

  const data = await res.json().catch(() => ({}))

  if (!res.ok || !data.success) {
    const message = data.message || '登录失败，请稍后重试'
    throw new Error(message)
  }

  // 后端返回：{ success: true, user: { ... } }
  return data.user
}

