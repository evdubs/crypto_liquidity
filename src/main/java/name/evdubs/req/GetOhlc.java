package name.evdubs.req;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import name.evdubs.Nonce;

public record GetOhlc(String pair, Integer interval, Instant since, long nonce) implements HasPayload {
  static Charset utf8 = StandardCharsets.UTF_8;
  
  public GetOhlc(String pair, Integer interval, Instant since) {
    this(pair, interval, since, Nonce.get());
  }

  @Override
  public long getNonce() {
    return nonce;
  }

  @Override
  public String getPayload() {
    return "nonce=" + Long.toString(nonce) + "&" +
      "pair=" + URLEncoder.encode(pair, utf8) + "&" +
      "interval=" + interval.toString() + "&" +
      "since=" + Long.toString(since.getEpochSecond());
  }

  @Override
  public String getUrl() {
    return "/0/public/OHLC";
  }
}
