package name.evdubs.rsp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONObject;

public record OpenOrder(
  String transactionId,
  String referralId,
  Integer userReference,
  OrderStatus status,
  Instant openTime,
  Instant startTime,
  Instant expireTime,
  String assetPair,
  OrderAction action,
  OrderType type,
  BigDecimal price,
  BigDecimal priceTwo,
  String leverageRatio,
  String description,
  String close,
  BigDecimal volume,
  BigDecimal executedVolume,
  BigDecimal executedCost,
  BigDecimal executedFee,
  BigDecimal executedPrice,
  BigDecimal stopPrice,
  BigDecimal limitPrice,
  OrderTrigger trigger,
  List<String> miscellaneous,
  List<String> flags,
  List<String> trades) {

  public OpenOrder(String transactionId, JSONObject jo) {
    this(transactionId,
      !jo.isNull("refid") ? jo.getString("refid") : null,
      jo.getInt("userref"),
      OrderStatus.fromString(jo.getString("status")),
      Instant.ofEpochMilli((long) (jo.getDouble("opentm") * 1000)),
      Instant.ofEpochMilli((long) (jo.getDouble("starttm") * 1000)),
      Instant.ofEpochMilli((long) (jo.getDouble("expiretm") * 1000)),
      jo.getJSONObject("descr").getString("pair"),
      OrderAction.fromString(jo.getJSONObject("descr").getString("type")),
      OrderType.fromString(jo.getJSONObject("descr").getString("ordertype")),
      jo.getJSONObject("descr").getBigDecimal("price"),
      jo.getJSONObject("descr").getBigDecimal("price2"),
      jo.getJSONObject("descr").getString("leverage"),
      jo.getJSONObject("descr").getString("order"),
      jo.getJSONObject("descr").getString("close"),
      jo.getBigDecimal("vol"),
      jo.getBigDecimal("vol_exec"),
      jo.getBigDecimal("cost"),
      jo.getBigDecimal("fee"),
      jo.getBigDecimal("price"),
      jo.getBigDecimal("stopprice"),
      jo.getBigDecimal("limitprice"),
      jo.has("trades") ? OrderTrigger.fromString(jo.getString("trigger")) : null,
      Arrays.asList(jo.getString("misc").split(",")),
      Arrays.asList(jo.getString("oflags")),
      jo.has("trades") ? jo.getJSONArray("trades").
        toList().
        stream().
        map(t -> t.toString()).
        collect(Collectors.toList()) : new ArrayList<String>());
  }
}
