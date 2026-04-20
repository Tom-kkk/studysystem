import { ref } from 'vue'

const STORAGE_KEY = 'student_login_user'

function readUserFromStorage() {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch (e) {
    return null
  }
}

export const studentLoginUser = ref(readUserFromStorage())

export function setStudentLoginUser(user) {
  studentLoginUser.value = user
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(user))
}

export function clearStudentLoginUser() {
  studentLoginUser.value = null
  sessionStorage.removeItem(STORAGE_KEY)
}

