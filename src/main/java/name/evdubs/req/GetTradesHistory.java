package name.evdubs.req;

import java.time.Instant;

import name.evdubs.Nonce;

public record GetTradesHistory(Instant start, Instant end, long nonce) implements HasPayload {
  public GetTradesHistory(Instant start, Instant end) {
    this(start, end, Nonce.get());
  }

  @Override
  public long getNonce() {
    return nonce;
  }

  @Override
  public String getPayload() {
    return "nonce=" + Long.toString(nonce) + 
      "&start=" + Long.toString(start.getEpochSecond()) +
      "&end=" + Long.toString(end.getEpochSecond());
  }

  @Override
  public String getUrl() {
    return "/0/private/TradesHistory";
  }
}
