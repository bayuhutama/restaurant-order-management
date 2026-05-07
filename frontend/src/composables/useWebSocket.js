import { ref, onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

/**
 * Composable that wraps the STOMP WebSocket client.
 *
 * Usage:
 *   const { connect, subscribe, disconnect, connected } = useWebSocket()
 *   connect((client) => {
 *     subscribe('/topic/orders', (order) => { ... })
 *   })
 *
 * The client reconnects automatically after 5 seconds if the connection drops.
 * All subscriptions are cleaned up automatically when the component is unmounted.
 *
 * Note: SockJS requires `global` to be defined. The Vite config sets
 * `define: { global: 'globalThis' }` to satisfy this requirement.
 */
export function useWebSocket() {
  const client = ref(null)
  const connected = ref(false)
  const subscriptions = []  // tracks active subscriptions for cleanup

  /**
   * Opens the WebSocket connection.
   * If a connection already exists (e.g. from a rapid remount), returns without
   * creating a duplicate client — prevents orphaned connections and duplicate
   * message handlers.
   *
   * @param onConnected - callback invoked once the STOMP session is established;
   *                      receives the client instance so subscriptions can be set up.
   */
  function connect(onConnected) {
    if (client.value) return  // already connected or connecting — avoid duplicates

    client.value = new Client({
      webSocketFactory: () => new SockJS(import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws'),
      reconnectDelay: 5000,  // retry after 5s on disconnect
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

  /**
   * Subscribes to a STOMP destination and parses incoming messages as JSON.
   * @param destination - e.g. '/topic/orders' or '/topic/orders/ORD-20240101-ABC123'
   * @param callback    - called with the parsed message body on each received message
   * @returns the subscription object, or null if not connected
   */
  function subscribe(destination, callback) {
    if (!client.value || !connected.value) return null
    const sub = client.value.subscribe(destination, (message) => {
      try {
        callback(JSON.parse(message.body))
      } catch (e) {
        console.error(`Failed to parse WebSocket message on ${destination}:`, e)
      }
    })
    subscriptions.push(sub)
    return sub
  }

  /** Unsubscribes from all active subscriptions and deactivates the client. */
  function disconnect() {
    subscriptions.forEach(sub => { try { sub.unsubscribe() } catch {} })
    subscriptions.length = 0  // clear the array so stale refs don't accumulate
    if (client.value) {
      client.value.deactivate()
      client.value = null  // allow connect() to create a new client if called again
    }
    connected.value = false
  }

  // Auto-disconnect when the component using this composable is destroyed
  onUnmounted(disconnect)

  return { connect, subscribe, disconnect, connected, client }
}
