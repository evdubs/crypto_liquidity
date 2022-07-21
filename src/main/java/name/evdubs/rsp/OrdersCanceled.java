package name.evdubs.rsp;

import org.json.JSONObject;

public record OrdersCanceled(int count) {
  public OrdersCanceled(JSONObject jo) {
    this(jo.getInt("count"));
  }
}
