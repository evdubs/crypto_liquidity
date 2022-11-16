package name.evdubs.req;

import name.evdubs.Nonce;

public record GetOrderBook(String pair, int count, long nonce) implements HasPayload {
  public GetOrderBook(String pair) {
    this(pair, 1, Nonce.get());
  }

  public GetOrderBook(String pair, int count) {
    this(pair, count, Nonce.get());
  }

  @Override
  public long getNonce() {
    return nonce;
  }

  @Override
  public String getPayload() {
    return "nonce=" + Long.toString(nonce) + 
      "&pair=" + pair + 
      "&count=" + Integer.toString(count);
  }

  @Override
  public String getUrl() {
    return "/0/public/Depth";
  }
  
}
