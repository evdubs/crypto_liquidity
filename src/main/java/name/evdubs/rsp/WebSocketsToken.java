package name.evdubs.rsp;

import org.json.JSONObject;

public record WebSocketsToken(String token) {
  public WebSocketsToken(JSONObject jo) {
    this(jo.getJSONObject("result").getString("token"));
  }
}
