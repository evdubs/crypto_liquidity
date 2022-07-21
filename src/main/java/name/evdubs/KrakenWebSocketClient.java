package name.evdubs;

public class KrakenWebSocketClient {
  /*
    WebSocket.Listener listener = new WebSocket.Listener() {
      List<CharSequence> parts = new ArrayList<>();
      CompletableFuture<?> accumulatedMessage = new CompletableFuture<>();
      
      public CompletionStage<?> onText(WebSocket ws, CharSequence cs, boolean last) {
        parts.add(cs);
        ws.request(1);
        if (last) {
            System.out.println(parts.toString());

            parts = new ArrayList<>();
            accumulatedMessage.complete(null);
            CompletionStage<?> cf = accumulatedMessage;
            accumulatedMessage = new CompletableFuture<>();
            return cf;
        }
        return accumulatedMessage;
      }
    };

    try {
      CompletableFuture<WebSocket> wsCf = HttpClient.
        newHttpClient().
        newWebSocketBuilder().
        buildAsync(new URI("wss://ws.kraken.com"), listener);

      WebSocket ws = wsCf.join();

      ws.sendText("""
          {
            \"event\": \"subscribe\",
            \"pair\": [
              \"ETH/XBT\"
            ],
            \"subscription\": {
              \"name\": \"book\"
            }
          }
          """, true);
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }

    while (true) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
   */
}
