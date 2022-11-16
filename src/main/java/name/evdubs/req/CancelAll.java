package name.evdubs.req;

import name.evdubs.Nonce;

public record CancelAll(long nonce) implements HasPayload {
  public CancelAll() {
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
    return "/0/private/CancelAll";
  }
}
