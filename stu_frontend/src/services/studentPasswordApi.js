export async function sendEmailCheckcode(email) {
  const formData = new FormData()
  formData.append('email', email)

  const res = await fetch('/api/email/checkcode', {
    method: 'POST',
    body: formData,
  })
  const data = await res.json().catch(() => ({}))
  if (!res.ok || !data.success) {
    throw new Error(data.message || '验证码发送失败')
  }
  return data
}

export async function forgetPassword({
  userName,
  email,
  emailCheckcode,
  newPass,
  confirmPass,
}) {
  const res = await fetch('/api/auth/forget-password', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      userName,
      email,
      emailCheckcode,
      newPass,
      confirmPass,
    }),
  })
  const data = await res.json().catch(() => ({}))
  if (!res.ok || !data.success) {
    throw new Error(data.message || '重置密码失败')
  }
  return data
}

