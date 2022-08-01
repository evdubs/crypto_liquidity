package name.evdubs.req;

import java.math.BigDecimal;
import java.math.MathContext;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import name.evdubs.Nonce;

public record AddOrder(
  String orderType,
  String pair,
  BigDecimal price,
  String type,
  BigDecimal volume,
  long nonce) implements HasPayload {
  static MathContext mc4 = new MathContext(4);
  static Charset utf8 = StandardCharsets.UTF_8;

  public AddOrder(String orderType, String pair, BigDecimal price, String type, BigDecimal volume) {
    this(orderType,
      pair,
      price.round(mc4),
      type,
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
      "ordertype=" + URLEncoder.encode(orderType, utf8) + "&" +
      "pair=" + URLEncoder.encode(pair, utf8) + "&" +
      "price=" + price.toPlainString() + "&" +
      "type=" + URLEncoder.encode(type, utf8) + "&" +
      "volume=" + volume.toPlainString() + "&" +
      "oflags=post";
  }
  
  @Override
  public String getUrl() {
    return "/0/private/AddOrder";
  }
}
