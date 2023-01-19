package name.evdubs.rsp;

import java.math.BigDecimal;
import java.time.Instant;

import org.json.JSONArray;

public record Ohlc(
  String pair,
  Instant time,
  BigDecimal open,
  BigDecimal high,
  BigDecimal low,
  BigDecimal close,
  BigDecimal vwap,
  BigDecimal volume,
  int count
) {
  public Ohlc(String pair, JSONArray ja) {
    this(
      pair,
      Instant.ofEpochSecond(ja.getLong(0)),
      ja.getBigDecimal(1),
      ja.getBigDecimal(2),
      ja.getBigDecimal(3),
      ja.getBigDecimal(4),
      ja.getBigDecimal(5),
      ja.getBigDecimal(6),
      ja.getInt(7)
    );
  }
}
