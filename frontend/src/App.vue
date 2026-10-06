<script setup>
import { RouterLink, RouterView } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { useCart } from '@/composables/useCart'

const { isLoggedIn } = useAuth()
const { count } = useCart()
</script>

<template>
  <div class="shell">
    <header class="topbar">
      <RouterLink to="/" class="brand">書店</RouterLink>
      <nav class="nav">
        <RouterLink to="/cart">
          購物車
          <span v-if="count" class="badge">{{ count }}</span>
        </RouterLink>
        <RouterLink v-if="isLoggedIn" to="/orders">訂單</RouterLink>
        <RouterLink v-if="isLoggedIn" to="/account">帳號</RouterLink>
        <RouterLink v-else to="/login">登入</RouterLink>
      </nav>
    </header>
    <RouterView />
  </div>
</template>

<style scoped>
.shell {
  width: min(1120px, calc(100% - 2rem));
  margin: 0 auto;
  padding-bottom: 3.5rem;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 1rem;
  min-height: 4.25rem;
  background: color-mix(in srgb, var(--bg) 88%, transparent);
  backdrop-filter: blur(12px);
}

.brand {
  font-family: var(--font-serif);
  font-size: 1.7rem;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.nav {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  margin-left: auto;
}

.nav a {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.4rem 0.8rem;
  border-radius: 999px;
  color: var(--muted);
}

.nav a.router-link-active {
  color: var(--ink);
  background: var(--bg-elevated);
}

.badge {
  min-width: 1.25rem;
  padding: 0 0.35rem;
  border-radius: 999px;
  background: var(--accent);
  color: #fffdf8;
  font-size: 0.75rem;
  line-height: 1.25rem;
  text-align: center;
}

@media (max-width: 640px) {
  .topbar {
    flex-wrap: wrap;
    padding: 0.75rem 0;
  }

  .nav {
    width: 100%;
    margin-left: 0;
  }
}
</style>
