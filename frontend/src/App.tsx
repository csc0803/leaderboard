import { useEffect } from 'react'

function App() {
  // 暫時用來確認前後端連線與 CORS，Phase 7 會改成 src/api 的 API service
  useEffect(() => {
    fetch(`${import.meta.env.VITE_API_BASE_URL}/api/posts`)
      .then(res => {
        console.log('status:', res.status)
        return res.json()
      })
      .then(data => console.log('posts:', data))
      .catch(err => console.error('fetch failed:', err))
  }, [])

  return <h1>Leaderboard</h1>
}

export default App
