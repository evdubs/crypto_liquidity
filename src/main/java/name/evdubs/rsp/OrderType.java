package name.evdubs.rsp;

public enum OrderType {
  MARKET("market"),
  LIMIT("limit"),
  STOP_LOSS("stop-loss"),
  TAKE_PROFIT("take-profit"),
  STOP_LOSS_LIMIT("stop-loss-limit"),
  TAKE_PROFIT_LIMIT("take-profit-limit"),
  SETTLE_POSITION("settle-position");

  String type;

  OrderType(String type) {
    this.type = type;
  }

  public String toKebabCase() {
    return type;
  }

  public static OrderType fromString(String s) {
    switch (s) {
      case "m":
        return MARKET;
      case "market":
        return MARKET;
      case "l":
        return LIMIT;
      case "limit":
        return LIMIT;
      case "stop-loss":
        return STOP_LOSS;
      case "take-profit":
        return TAKE_PROFIT;
      case "stop-loss-limit":
        return STOP_LOSS_LIMIT;
      case "take-profit-limit":
        return TAKE_PROFIT_LIMIT;
      case "settle-position":
        return SETTLE_POSITION;
      default:
        return null;
    }
  }
}
