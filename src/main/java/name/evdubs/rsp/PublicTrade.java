package name.evdubs.rsp;

import java.math.BigDecimal;
import java.time.Instant;

import org.json.JSONArray;

public record PublicTrade(
  String pair, 
  BigDecimal price,
  BigDecimal volume,
  Instant time,
  OrderAction action,
  OrderType type,
  String miscellaneous,
  Long tradeId
  ) {
  public PublicTrade(String pair, JSONArray ja) {
    this(
      pair,
      ja.getBigDecimal(0),
      ja.getBigDecimal(1),
      Instant.ofEpochSecond(ja.getLong(2)),
      OrderAction.fromString(ja.getString(3)),
      OrderType.fromString(ja.getString(4)),
      ja.getString(5),
      ja.getLong(6)
    );
  }
}
