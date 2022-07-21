package name.evdubs.req;

import name.evdubs.Nonce;

public class GetOpenOrders implements HasPayload {
  private long nonce;

  private boolean trades;

  private String userRef;

  public GetOpenOrders() {
    this.nonce = Nonce.get();
    this.trades = false;
    this.userRef = "";
  }
 
  public GetOpenOrders(boolean trades) {
    this.nonce = Nonce.get();
    this.trades = trades;
    this.userRef = "";
  }

  public GetOpenOrders(String userRef) {
    this.nonce = Nonce.get();
    this.trades = false;
    this.userRef = userRef;
  }

  public GetOpenOrders(boolean trades, String userRef) {
    this.nonce = Nonce.get();
    this.trades = trades;
    this.userRef = userRef;
  }

  @Override
  public long getNonce() {
    return nonce;
  }

  @Override
  public String getPayload() {
    return "nonce=" + Long.toString(nonce) + 
      "&trades=" + Boolean.toString(trades) + 
      (userRef == null || userRef == "" ? "" : "&userref=" + userRef);
  }

  @Override
  public String getUrl() {
    return "/0/private/OpenOrders";
  }
  
}
