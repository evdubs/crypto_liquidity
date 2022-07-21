package name.evdubs.rsp;

import java.util.List;

import org.json.JSONObject;

public record OrderAdded(
  String description,
  String closeDescription,
  List<String> transactionIds
) {
  public OrderAdded(JSONObject jo) {
    this(jo.getJSONObject("descr").getString("order"),
      jo.getJSONObject("descr").has("close") ? 
        jo.getJSONObject("descr").getString("close") : null,
      jo.getJSONArray("txid").
        toList().
        stream().
        map(id -> id.toString()).
        toList());
  }
}
