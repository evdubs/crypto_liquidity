package name.evdubs.req;

import name.evdubs.Nonce;

public record GetOpenOrders(boolean trades, String userRef, long nonce) implements HasPayload {
  public GetOpenOrders() {
    this(false, "", Nonce.get());
  }
 
  public GetOpenOrders(boolean trades) {
    this(trades, "", Nonce.get());
  }

  public GetOpenOrders(String userRef) {
    this(false, userRef, Nonce.get());
  }

  public GetOpenOrders(boolean trades, String userRef) {
    this(trades, userRef, Nonce.get());
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
