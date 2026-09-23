import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const websocketService = {
  connect(onUpdate) {
    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:3000/ws'),
      reconnectDelay: 5000,
      onConnect: () => {
        this.client = client;
        this.subscriptions = new Map();
        this.queueIds?.forEach((queueId) => this.subscribe(queueId));
      },
      onStompError: (frame) => {
        console.error('[WebSocket] STOMP error:', frame.headers.message);
      },
    });

    client.activate();
    this.client = client;
    this.onUpdate = onUpdate;
    this.queueIds = new Set();
    return client;
  },

  subscribe(queueId) {
    this.queueIds?.add(queueId);

    if (!this.client?.connected || this.subscriptions?.has(queueId)) {
      return;
    }

    const subscription = this.client.subscribe(
      `/topic/queue/${queueId}/updates`,
      (message) => {
        try {
          this.onUpdate?.(JSON.parse(message.body));
        } catch (error) {
          console.error('[WebSocket] Invalid queue update:', error);
        }
      },
    );

    this.subscriptions.set(queueId, subscription);
  },

  disconnect() {
    this.subscriptions?.forEach((subscription) => subscription.unsubscribe());
    this.subscriptions?.clear();
    this.queueIds?.clear();
    this.client?.deactivate();
    this.client = null;
    this.onUpdate = null;
  },
};

export default websocketService;
