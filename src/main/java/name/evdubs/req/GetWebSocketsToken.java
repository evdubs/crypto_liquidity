package name.evdubs.req;

import name.evdubs.Nonce;

public class GetWebSocketsToken implements HasPayload {
  private long nonce;

  public GetWebSocketsToken() {
    this.nonce = Nonce.get();
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
    return "/0/private/GetWebSocketsToken";
  }
}
