package name.evdubs.rsp;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONObject;

public class AccountBalance {
  Map<String, BigDecimal> balances = new HashMap<String, BigDecimal>();

  public AccountBalance(JSONObject jo) {
    balances = jo.getJSONObject("result").
      toMap().
      entrySet().
      stream().
      collect(Collectors.toMap(sb -> sb.getKey().toString(), 
        sb -> new BigDecimal(sb.getValue().toString())));
  }

  public BigDecimal getBalance(String symbol) {
    return balances.getOrDefault(symbol, BigDecimal.ZERO);
  }

  @Override
  public String toString() {
    return "AccountBalance [balances=" + balances + "]";
  }
}
