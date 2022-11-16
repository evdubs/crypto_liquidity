package name.evdubs;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.List;

import name.evdubs.req.GetTradesHistory;
import name.evdubs.rsp.Trade;

public class SaveTrades {
  static String apiKey = System.getenv("API_KEY");

  static String apiSecret = System.getenv("API_SECRET");

  static String dbStr = System.getenv("DB");
  
  private static void persistTrades(Connection db, List<Trade> trades) throws SQLException {
    PreparedStatement st = db.prepareStatement("""
insert into
  kraken.trade
(
  transaction_id,
  order_transaction_id,
  pair,
  timestamp,
  action,
  order_type,
  price,
  cost,
  fee,
  volume
) values (
  ?,
  ?,
  ?,
  ?::text::timestamptz,
  ?::text::kraken.action,
  ?::text::kraken.order_type,
  ?,
  ?,
  ?,
  ?
) on conflict (transaction_id) do nothing;
    """);

    for (Trade t : trades) {
      st.setString(1, t.transactionId());
      st.setString(2, t.orderTransactionId());
      st.setString(3, t.pair());
      st.setString(4, t.time().toString());
      st.setString(5, t.type());
      st.setString(6, t.orderType());
      st.setBigDecimal(7, t.price());
      st.setBigDecimal(8, t.cost());
      st.setBigDecimal(9, t.fee());
      st.setBigDecimal(10, t.volume());
      st.addBatch();
    }

    st.executeBatch();

    System.out.println("Saved " + trades.size() + " trades");
  }

  public static void main(String[] args) {
    var kraken = new KrakenHttpClient(apiKey, apiSecret);
    var start = Instant.now().minus(2, ChronoUnit.DAYS);
    var end = Instant.now();

    try {
      var db = DriverManager.getConnection(dbStr);

      var trades = kraken.getTradesHistory(new GetTradesHistory(start, end));

      persistTrades(db, trades);

      // getTradesHistory will only return 50 trades max; attempt to get the rest here
      while (trades.size() == 50) {
        var earliest = trades.stream().map(Trade::time).min(Instant::compareTo).get();

        System.out.println("earliest: " + earliest);

        trades = kraken.getTradesHistory(new GetTradesHistory(start, earliest));

        persistTrades(db, trades);
      }
    } catch (KrakenException | SQLException e) {
      e.printStackTrace();
    }
  }
}
