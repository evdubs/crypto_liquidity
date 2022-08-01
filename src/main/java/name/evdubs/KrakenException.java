package name.evdubs;

public class KrakenException extends Exception {
  public KrakenException() {
    super();
  }

  public KrakenException(String message) {
    super(message);
  }

  public KrakenException(String message, Throwable cause) {
    super(message, cause);
  }

  public KrakenException(Throwable cause) {
    super(cause);
  }
}
