<script setup>
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()
const { email, name, logout } = useAuth()

function signOut() {
  logout()
  router.push('/')
}
</script>

<template>
  <main class="page">
    <h1>帳號</h1>
    <p v-if="name" class="name">{{ name }}</p>
    <p class="email">{{ email }}</p>
    <div class="actions">
      <RouterLink to="/orders">查看訂單</RouterLink>
      <button type="button" @click="signOut">登出</button>
    </div>
  </main>
</template>

<style scoped>
.page {
  max-width: 32rem;
  padding-top: 2rem;
}

h1,
.name {
  font-family: var(--font-serif);
}

h1 {
  font-size: 2.2rem;
}

.name {
  margin-top: 1rem;
  font-size: 1.4rem;
}

.email {
  margin-top: 0.35rem;
  color: var(--muted);
}

.actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 1.5rem;
}

.actions a,
.actions button {
  border: 1px solid var(--line);
  border-radius: 999px;
  padding: 0.55rem 0.95rem;
  background: var(--bg-elevated);
  color: var(--ink);
  cursor: pointer;
  font-weight: 600;
  transition: all var(--transition-base);
}

.actions a:hover {
  background: var(--accent);
  border-color: var(--accent);
  color: #fff;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(212, 101, 74, 0.3);
}

.actions button {
  background: transparent;
}

.actions button:hover {
  border-color: var(--accent);
  color: var(--accent);
  background: rgba(212, 101, 74, 0.05);
}
</style>
