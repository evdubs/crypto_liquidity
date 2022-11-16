package name.evdubs.req;

import name.evdubs.Nonce;

public record GetAccountBalance(long nonce) implements HasPayload {
  public GetAccountBalance() {
    this(Nonce.get());
  }

  @Override
  public long getNonce() {
    return nonce;
  }

  @Override
  public String getPayload() {
    return "nonce=" + Long.toString(nonce);
  }

  @Override
  public String getUrl() {
    return "/0/private/Balance";
  }
  
}
