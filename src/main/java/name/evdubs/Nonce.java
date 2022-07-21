package name.evdubs;

import java.time.Instant;

public class Nonce {
  static long incr = 0;

  public static long get() {
    if (incr >= 1000)
      incr = 0;

    long millis = Instant.now().toEpochMilli();

    return millis * 1000 + ++incr;
  }
}
