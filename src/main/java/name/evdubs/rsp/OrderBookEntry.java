package name.evdubs.rsp;

import java.math.BigDecimal;
import java.time.Instant;

import org.json.JSONArray;

public record OrderBookEntry (
  BigDecimal price,
  BigDecimal volume,
  Instant timestamp
) {
  public OrderBookEntry(JSONArray ja) {
    this(ja.getBigDecimal(0),
      ja.getBigDecimal(1),
      Instant.ofEpochMilli(ja.getLong(2) * 1000));
  }
}
