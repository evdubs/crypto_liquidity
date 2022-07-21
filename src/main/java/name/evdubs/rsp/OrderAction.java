package name.evdubs.rsp;

public enum OrderAction {
  BUY("buy"),
  SELL("sell");
  
  String action;

  OrderAction(String action) {
    this.action = action;
  }

  public String getLowercaseString() {
    return action;
  }

  public static OrderAction fromString(String s) {
    switch (s) {
      case "buy":
        return BUY;
      case "sell":
        return SELL;
      default:
        return null;
    }
  }
}
