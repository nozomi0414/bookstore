const BASE = 'http://localhost:8080'

export async function api(path, { method = 'GET', body, token } = {}) {
  let response
  try {
    const options = {
      method,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    }
    
    if (body && method !== 'GET') {
      options.body = JSON.stringify(body)
    }

    response = await fetch(`${BASE}${path}`, options)
  } catch {
    throw new Error('無法連上後端，請確認 http://localhost:8080 已啟動')
  }

  const data = response.status === 204 ? null : await response.json().catch(() => null)
  if (!response.ok) {
    throw new Error(data?.error || '請求失敗')
  }
  return data
}

const ERRORS = {
  'Email already exists': '這個電子郵件已經註冊過',
  'Email or password is incorrect': '電子郵件或密碼不正確',
  'Order lines are required': '請先把書放進購物車',
  'Book not found': '找不到這本書',
  'Quantity must be at least 1': '數量至少要 1',
  'Invalid authorization': '請先登入',
  'Invalid or expired token': '登入已過期，請重新登入',
  'User not found': '找不到這個帳號',
}

export function describeError(error) {
  const message = error instanceof Error ? error.message : String(error)
  return ERRORS[message] || message
}
