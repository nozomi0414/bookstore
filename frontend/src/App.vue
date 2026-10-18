<script setup>
import { RouterLink, RouterView } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { useCart } from '@/composables/useCart'
import { useToast } from '@/composables/useToast'

const { isLoggedIn, isAdmin } = useAuth()
const { count } = useCart()
const { message } = useToast()
</script>

<template>
  <header class="topbar">
    <div class="topbar-inner">
      <RouterLink to="/" class="brand">書店</RouterLink>
      <nav class="nav">
        <RouterLink to="/cart">
          購物車
          <span v-if="count" class="badge">{{ count }}</span>
        </RouterLink>
        <RouterLink v-if="isAdmin" to="/books/new">上架</RouterLink>
        <RouterLink v-if="isLoggedIn" to="/orders">訂單</RouterLink>
        <RouterLink v-if="isLoggedIn" to="/orders/history">歷史訂單</RouterLink>
        <RouterLink v-if="isLoggedIn" to="/account">帳號</RouterLink>
        <RouterLink v-else to="/login">登入</RouterLink>
      </nav>
    </div>
  </header>
  <div class="shell">
    <RouterView />
  </div>
  <p v-if="message" class="toast" role="status">{{ message }}</p>
</template>

<style scoped>
.shell {
  width: min(1560px, calc(100% - 3rem));
  margin: 0 auto;
  padding-top: 5.75rem;
  padding-bottom: 4rem;
}

.topbar {
  position: fixed;
  top: 0;
  right: 0;
  left: 0;
  z-index: 20;
  background: #fffdfb;
  border-bottom: 3px solid #f0c2b4;
  box-shadow: 0 8px 24px rgba(120, 78, 52, 0.06);
}

.topbar-inner {
  display: flex;
  align-items: center;
  gap: 1rem;
  width: min(1560px, calc(100% - 3rem));
  min-height: 4.5rem;
  margin: 0 auto;
}

.brand {
  padding: 0.15rem 0.75rem;
  border-radius: 12px;
  background: #f6d5cc;
  color: #8d3d2c;
  font-family: var(--font-serif);
  font-size: 1.7rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  transition: all var(--transition-base);
}

.brand:hover {
  background: #f3c5b8;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(141, 61, 44, 0.15);
}

.nav {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  margin-left: auto;
}

.nav a {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.4rem 0.8rem;
  border-radius: 999px;
  color: var(--muted);
  transition: all var(--transition-base);
}

.nav a:hover {
  color: var(--ink);
  background: color-mix(in srgb, var(--bg-elevated) 60%, transparent);
}

.nav a.router-link-active {
<<<<<<< HEAD
  background: #f6d5cc;
  color: #8d3d2c;
  font-weight: 700;
=======
  color: var(--ink);
  background: var(--bg-elevated);
  box-shadow: 0 2px 8px rgba(120, 78, 52, 0.08);
>>>>>>> a4bc78dffc644df7f3fba307cf2b5f510a938bac
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

.toast {
  position: fixed;
  top: 5.4rem;
  right: 1.25rem;
  z-index: 30;
  max-width: min(22rem, calc(100% - 2rem));
  padding: 0.85rem 1rem;
  border-radius: 16px;
  background: var(--bg-elevated);
  color: var(--ink);
  box-shadow: var(--shadow);
  font-weight: 700;
}

@media (max-width: 640px) {
  .shell {
    padding-top: 7.25rem;
  }

  .topbar-inner {
    flex-wrap: wrap;
    padding: 0.75rem 0;
  }

  .nav {
    width: 100%;
    margin-left: 0;
  }
}
</style>
