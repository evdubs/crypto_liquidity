package name.evdubs.rsp;

public enum OrderTrigger {
  LAST("last"),
  INDEX("index");

  String trigger;

  OrderTrigger(String trigger) {
    this.trigger = trigger;
  }

  public String getLowercaseString() {
    return trigger;
  }

  public static OrderTrigger fromString(String s) {
    switch (s) {
      case "last":
        return LAST;
      case "index":
        return INDEX;
      default:
        return null;
    }
  }
}
