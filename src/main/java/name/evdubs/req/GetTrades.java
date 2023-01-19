package name.evdubs.req;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import name.evdubs.Nonce;

public record GetTrades(String pair, Instant since, long nonce) implements HasPayload {
  static Charset utf8 = StandardCharsets.UTF_8;

  public GetTrades(String pair, Instant since) {
    this(pair, since, Nonce.get());
  }

  @Override
  public long getNonce() {
    return nonce;
  }


  @Override
  public String getPayload() {
    return "nonce=" + Long.toString(nonce) + "&" +
      "pair=" + URLEncoder.encode(pair, utf8) + "&" +
      "since=" + Long.toString(since.getEpochSecond());
  }


  @Override
  public String getUrl() {
    return "/0/public/Trades";
  }
}
