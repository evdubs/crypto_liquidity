package name.evdubs.rsp;

public enum OrderStatus {
  PENDING("pending"),
  OPEN("open"),
  CLOSED("closed"),
  CANCELED("canceled"),
  EXPIRED("expired");

  String status;

  private OrderStatus(String status) {
    this.status = status;
  }

  public String getLowercaseString() {
    return status;
  }

  public static OrderStatus fromString(String s) {
    switch (s) {
      case "pending":
        return PENDING;
      case "open":
        return OPEN;
      case "closed":
        return CLOSED;
      case "canceled":
        return CANCELED;
      case "expired":
        return EXPIRED;
      default:
        return null;
    }
  }
}
