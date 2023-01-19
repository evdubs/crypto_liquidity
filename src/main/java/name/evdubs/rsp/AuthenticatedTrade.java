package name.evdubs.rsp;

import java.math.BigDecimal;
import java.time.Instant;

import org.json.JSONObject;

public record AuthenticatedTrade(
  String transactionId,
  String orderTransactionId,
  String pair,
  Instant time,
  String type,
  String orderType,
  BigDecimal price,
  BigDecimal cost,
  BigDecimal fee,
  BigDecimal volume
) {
  public AuthenticatedTrade(String transactionId, JSONObject jo) {
    this(transactionId,
      jo.getString("ordertxid"),
      jo.getString("pair"),
      Instant.ofEpochSecond(jo.getLong("time")),
      jo.getString("type"),
      jo.getString("ordertype"),
      jo.getBigDecimal("price"),
      jo.getBigDecimal("cost"),
      jo.getBigDecimal("fee"),
      jo.getBigDecimal("vol")
    );
  }
}
