package name.evdubs.req;

import name.evdubs.Nonce;

public class GetOrderBook implements HasPayload {
  private long nonce;

  private String pair;

  private int count;

  public GetOrderBook(String pair) {
    this.pair = pair;
    this.count = 1;
  }

  public GetOrderBook(String pair, int count) {
    this.nonce = Nonce.get();
    this.pair = pair;
    this.count = count;
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
