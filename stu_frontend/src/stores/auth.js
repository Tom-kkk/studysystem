import { ref } from 'vue'

const STORAGE_KEY = 'teacher_login_user'

function readUserFromStorage() {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch (e) {
    return null
  }
}

export const loginUser = ref(readUserFromStorage())

export function setLoginUser(user) {
  loginUser.value = user
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(user))
}

export function clearLoginUser() {
  loginUser.value = null
  sessionStorage.removeItem(STORAGE_KEY)
}

