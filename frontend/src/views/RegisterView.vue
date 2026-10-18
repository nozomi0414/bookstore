<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, describeError } from '@/api/client'
import { useAuth } from '@/composables/useAuth'

const route = useRoute()
const router = useRouter()
const { setSession } = useAuth()

const name = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const error = ref('')
const submitting = ref(false)

function nextPath() {
  const redirect = route.query.redirect
  if (typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//')) {
    return redirect
  }
  return '/'
}

async function submit() {
  error.value = ''
  if (!name.value || !email.value || !password.value || !confirmPassword.value) {
    error.value = '請填寫姓名、電子郵件、密碼和確認密碼'
    return
  }
  if (password.value !== confirmPassword.value) {
    error.value = '兩次輸入的密碼不一致'
    return
  }
  submitting.value = true
  try {
    const registered = await api('/api/users/register', {
      method: 'POST',
      body: { name: name.value, email: email.value, password: password.value },
    })
    const loggedIn = await api('/api/users/login', {
      method: 'POST',
      body: { email: email.value, password: password.value },
    })
    setSession({ token: loggedIn.token, email: registered.email, name: registered.name })
    router.push(nextPath())
  } catch (e) {
    error.value = describeError(e)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="panel">
    <h1>註冊</h1>
    <p class="lead">建立帳號後就能結帳、查看訂單。</p>
    <form @submit.prevent="submit">
      <label>
        姓名
        <input v-model="name" type="text" autocomplete="name" />
      </label>
      <label>
        電子郵件
        <input v-model="email" type="email" autocomplete="email" />
      </label>
      <label>
        密碼
        <input v-model="password" type="password" autocomplete="new-password" />
      </label>
      <label>
        確認密碼
        <input v-model="confirmPassword" type="password" autocomplete="new-password" />
      </label>
      <p v-if="error" class="error">{{ error }}</p>
      <button type="submit" :disabled="submitting">{{ submitting ? '建立中…' : '建立帳號' }}</button>
    </form>
    <p class="switch">
      已經有帳號？
      <RouterLink :to="{ name: 'login', query: route.query }">登入</RouterLink>
    </p>
  </main>
</template>

<style scoped>
.panel {
  max-width: 26rem;
  margin: 2.5rem auto 0;
}

h1 {
  font-family: var(--font-serif);
  font-size: 2.2rem;
}

.lead,
.switch {
  margin-top: 0.45rem;
  color: var(--muted);
}

form {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
  margin-top: 1.5rem;
}

label {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-size: 0.92rem;
}

input {
  border: 2px solid var(--line);
  border-radius: 12px;
  padding: 0.7rem 0.8rem;
  background: var(--bg-elevated);
  color: var(--ink);
  transition: all var(--transition-base);
}

input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(212, 101, 74, 0.1);
  outline: none;
}

button {
  margin-top: 0.3rem;
  border: 0;
  border-radius: 999px;
  padding: 0.7rem 1rem;
  background: var(--ink);
  color: var(--bg);
  cursor: pointer;
  font-weight: 700;
  transition: all var(--transition-base);
}

button:hover:not(:disabled) {
  background: var(--accent);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(212, 101, 74, 0.3);
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.error {
  color: var(--accent);
}

.switch a {
  color: var(--accent);
  font-weight: 700;
  transition: all var(--transition-base);
  text-decoration: underline;
  text-decoration-color: transparent;
}

.switch a:hover {
  text-decoration-color: var(--accent);
}
</style>
