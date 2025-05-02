package name.evdubs.rsp;

import java.math.BigDecimal;
import java.time.Instant;

import org.json.JSONObject;

public record LedgerEntry(
  String entryId,
  String refId,
  Instant time,
  String type,
  String subtype,
  String assetClass,
  String asset,
  BigDecimal amount,
  BigDecimal fee,
  BigDecimal balance
) {
  public LedgerEntry(String entryId, JSONObject jo) {
    this(entryId,
      jo.getString("refid"),
      Instant.ofEpochSecond(jo.getLong("time")),
      jo.getString("type"),
      jo.getString("subtype"),
      jo.getString("aclass"),
      jo.getString("asset"),
      jo.getBigDecimal("amount"),
      jo.getBigDecimal("fee"),
      jo.getBigDecimal("balance")
    );
  }
}
