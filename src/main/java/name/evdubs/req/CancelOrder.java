package name.evdubs.req;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import name.evdubs.Nonce;

public record CancelOrder(String transactionId, long nonce) implements HasPayload {
  static Charset utf8 = StandardCharsets.UTF_8;

  public CancelOrder(String transactionId) {
    this(transactionId, Nonce.get());
  }

  @Override
  public long getNonce() {
    return nonce;
  }

  @Override
  public String getPayload() {
    return "nonce=" + Long.toString(nonce) + "&" + 
      "txid=" + URLEncoder.encode(transactionId, utf8);
  }
  
  @Override
  public String getUrl() {
    return "/0/private/CancelOrder";
  }
}
