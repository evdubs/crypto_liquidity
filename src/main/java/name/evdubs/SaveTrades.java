package name.evdubs;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import name.evdubs.req.GetTrades;
import name.evdubs.req.GetTradesHistory;
import name.evdubs.rsp.AuthenticatedTrade;
import name.evdubs.rsp.PublicTrade;

public class SaveTrades {
  static String apiKey = System.getenv("API_KEY");

  static String apiSecret = System.getenv("API_SECRET");

  static String dbStr = System.getenv("DB");

  static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX").withZone(ZoneId.of("Z"));

  static void println(String s) {
    System.out.println(dtf.format(Instant.now()) + " " + s);
  }
  
  private static void persistTrades(Connection db, List<AuthenticatedTrade> trades) throws SQLException {
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

    for (AuthenticatedTrade t : trades) {
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

    println("Saved " + trades.size() + " trades");
  }

  private static List<PublicTrade> getTrades(KrakenHttpClient kraken, String pair, 
    Instant tradeTime, Instant startTime) throws KrakenException {
    var trades = kraken.getTrades(new GetTrades(pair, startTime));
    var minTime = trades.stream().min(Comparator.comparing(PublicTrade::time)).get();
    var maxTime = trades.stream().max(Comparator.comparing(PublicTrade::time)).get();

    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      println("Sleep interrupted");
    }

    if (minTime.time().compareTo(tradeTime) > 0) {
      println("Going back a minute for " + pair + " at " + 
        startTime.toString() + " to find " + tradeTime.toString());
      return getTrades(kraken, pair, tradeTime, startTime.minus(1, ChronoUnit.MINUTES));
    } else if (maxTime.time().compareTo(tradeTime) < 0) {
      println("Going forward a second for " + pair + " at " + 
        maxTime.time().toString() + " to find " + tradeTime.toString());
      return getTrades(kraken, pair, tradeTime, maxTime.time().plus(1, ChronoUnit.SECONDS));
    } else {
      return trades;
    }
  }

  private static void persistReferencePrices(Connection db, KrakenHttpClient kraken, List<AuthenticatedTrade> trades) 
    throws KrakenException, SQLException {
    PreparedStatement st = db.prepareStatement("""
insert into
  kraken.usd_reference_price
(
  transaction_id,
  base_currency_pair,
  base_currency_price,
  base_currency_volume,
  base_currency_timestamp,
  counter_currency_pair,
  counter_currency_price,
  counter_currency_volume,
  counter_currency_timestamp
) values (
  ?,
  ?,
  ?,
  ?,
  ?::text::timestamptz,
  ?,
  ?,
  ?,
  ?::text::timestamptz
) on conflict (transaction_id) do nothing;
    """);

    for (AuthenticatedTrade t : trades) {
      if ("XETHXXBT".equals(t.pair())) {
        var ethusd = getTrades(kraken, "XETHZUSD", t.time(), t.time().minus(3, ChronoUnit.MINUTES));
        var ethusdTrade = ethusd.
          stream().
          reduce(ethusd.get(0), (latest, trade) -> {
            if (trade.time().compareTo(t.time()) <= 0)
              return trade;
            else
              return latest;
          });

        var btcusd = getTrades(kraken, "XXBTZUSD", t.time(), t.time().minus(1, ChronoUnit.MINUTES));
        var btcusdTrade = btcusd.
          stream().
          reduce(btcusd.get(0), (latest, trade) -> {
            if (trade.time().compareTo(t.time()) <= 0)
              return trade;
            else
              return latest;
          });

        st.setString(1, t.transactionId());
        st.setString(2, ethusdTrade.pair());
        st.setBigDecimal(3, ethusdTrade.price());
        st.setBigDecimal(4, ethusdTrade.volume());
        st.setString(5, ethusdTrade.time().toString());
        st.setString(6, btcusdTrade.pair());
        st.setBigDecimal(7, btcusdTrade.price());
        st.setBigDecimal(8, btcusdTrade.volume());
        st.setString(9, btcusdTrade.time().toString());
        st.addBatch();
      }
    }

    st.executeBatch();

    println("Saved prices for " + trades.size() + " trades");
  }

  public static void main(String[] args) {
    var kraken = new KrakenHttpClient(apiKey, apiSecret);
    var start = Instant.now().minus(4, ChronoUnit.DAYS);
    var end = Instant.now();

    try {
      var db = DriverManager.getConnection(dbStr);

      var trades = kraken.getTradesHistory(new GetTradesHistory(start, end));

      persistTrades(db, trades);

      persistReferencePrices(db, kraken, trades);

      // getTradesHistory will only return 50 trades max; attempt to get the rest here
      while (trades.size() == 50) {
        var earliest = trades.stream().map(AuthenticatedTrade::time).min(Instant::compareTo).get();

        println("earliest: " + earliest);

        trades = kraken.getTradesHistory(new GetTradesHistory(start, earliest));

        persistTrades(db, trades);

        persistReferencePrices(db, kraken, trades);
      }
    } catch (KrakenException | SQLException e) {
      e.printStackTrace();
    }
  }
}
