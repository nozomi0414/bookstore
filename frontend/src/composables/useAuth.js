import { computed, ref } from 'vue'

const token = ref(localStorage.getItem('token') || '')
const email = ref(localStorage.getItem('email') || '')
const name = ref(localStorage.getItem('name') || '')

export function useAuth() {
  const isLoggedIn = computed(() => Boolean(token.value))

  function setSession({ token: nextToken, email: nextEmail, name: nextName = '' }) {
    token.value = nextToken
    email.value = nextEmail || ''
    name.value = nextName
    localStorage.setItem('token', token.value)
    localStorage.setItem('email', email.value)
    localStorage.setItem('name', name.value)
  }

  function logout() {
    token.value = ''
    email.value = ''
    name.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('email')
    localStorage.removeItem('name')
  }

  return { token, email, name, isLoggedIn, setSession, logout }
}
