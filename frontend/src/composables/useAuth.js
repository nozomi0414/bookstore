import { computed, ref } from 'vue'

const token = ref(localStorage.getItem('token') || '')
const email = ref(localStorage.getItem('email') || '')
const name = ref(localStorage.getItem('name') || '')
const role = ref(localStorage.getItem('role') || '')

export function useAuth() {
  const isLoggedIn = computed(() => Boolean(token.value))
  const isAdmin = computed(() => role.value === 'ADMIN')

  function setSession({ token: nextToken, email: nextEmail, name: nextName = '', role: nextRole = '' }) {
    token.value = nextToken
    email.value = nextEmail || ''
    name.value = nextName
    role.value = nextRole
    localStorage.setItem('token', token.value)
    localStorage.setItem('email', email.value)
    localStorage.setItem('name', name.value)
    localStorage.setItem('role', role.value)
  }

  function setName(nextName) {
    name.value = nextName || ''
    localStorage.setItem('name', name.value)
  }

  function logout() {
    token.value = ''
    email.value = ''
    name.value = ''
    role.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('email')
    localStorage.removeItem('name')
    localStorage.removeItem('role')
  }

  return { token, email, name, role, isLoggedIn, isAdmin, setSession, setName, logout }
}
