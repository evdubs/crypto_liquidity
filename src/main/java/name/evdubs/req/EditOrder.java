package name.evdubs.req;

import java.math.BigDecimal;
import java.math.MathContext;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import name.evdubs.Nonce;

public record EditOrder(
  String transactionId,
  String pair,
  BigDecimal price,
  BigDecimal volume,
  long nonce) implements HasPayload {
  static MathContext mc4 = new MathContext(4);
  static Charset utf8 = StandardCharsets.UTF_8;

  public EditOrder(String transactionId, String pair, BigDecimal price, BigDecimal volume) {
    this(transactionId,
      pair,
      price.round(mc4),
      volume.round(mc4),
      Nonce.get());
  }

  @Override
  public long getNonce() {
    return nonce;
  }

  @Override
  public String getPayload() {
    return "nonce=" + Long.toString(nonce) + "&" + 
      "txid=" + URLEncoder.encode(transactionId, utf8) + "&" +
      "pair=" + URLEncoder.encode(pair, utf8) + "&" +
      "price=" + price.toPlainString() + "&" +
      "volume=" + volume.toPlainString() + "&" +
      "oflags=post";
  }
  
  @Override
  public String getUrl() {
    return "/0/private/EditOrder";
  }
}
