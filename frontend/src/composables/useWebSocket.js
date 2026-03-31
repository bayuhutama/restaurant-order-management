import { ref, onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

export function useWebSocket() {
  const client = ref(null)
  const connected = ref(false)
  const subscriptions = []

  function connect(onConnected) {
    client.value = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      reconnectDelay: 5000,
      onConnect: () => {
        connected.value = true
        if (onConnected) onConnected(client.value)
      },
      onDisconnect: () => {
        connected.value = false
      },
      onStompError: (frame) => {
        console.error('WebSocket error:', frame)
      }
    })
    client.value.activate()
  }

  function subscribe(destination, callback) {
    if (!client.value || !connected.value) return null
    const sub = client.value.subscribe(destination, (message) => {
      callback(JSON.parse(message.body))
    })
    subscriptions.push(sub)
    return sub
  }

  function disconnect() {
    subscriptions.forEach(sub => { try { sub.unsubscribe() } catch {} })
    if (client.value) client.value.deactivate()
  }

  onUnmounted(disconnect)

  return { connect, subscribe, disconnect, connected, client }
}
