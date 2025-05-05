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

import name.evdubs.req.GetLedgersInfo;
import name.evdubs.req.GetTrades;
import name.evdubs.rsp.LedgerEntry;
import name.evdubs.rsp.PublicTrade;

public class SaveLedgerEntries {
  static String apiKey = System.getenv("API_KEY");

  static String apiSecret = System.getenv("API_SECRET");

  static String dbStr = System.getenv("DB");

  static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX").withZone(ZoneId.of("Z"));

  static void println(String s) {
    System.out.println(dtf.format(Instant.now()) + " " + s);
  }
  
  private static void persistLedgers(Connection db, List<LedgerEntry> ledgers) throws SQLException {
    PreparedStatement st = db.prepareStatement("""
insert into
  kraken.ledger_entry
(
  entry_id,
  reference_id,
  timestamp,
  entry_type,
  entry_subtype,
  asset_class,
  asset,
  amount,
  fee,
  balance
) values (
  ?,
  ?,
  ?::text::timestamptz,
  ?::text::kraken.entry_type,
  ?,
  ?,
  ?,
  ?,
  ?,
  ?
) on conflict (entry_id) do nothing;
    """);

    for (LedgerEntry l : ledgers) {
      st.setString(1, l.entryId());
      st.setString(2, l.refId());
      st.setString(3, l.time().toString());
      st.setString(4, l.type());
      st.setString(5, l.subtype());
      st.setString(6, l.assetClass());
      st.setString(7, l.asset());
      st.setBigDecimal(8, l.amount());
      st.setBigDecimal(9, l.fee());
      st.setBigDecimal(10, l.balance());
      st.addBatch();
    }

    st.executeBatch();

    println("Saved " + ledgers.size() + " ledger entries");
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

  private static void persistReferencePrices(Connection db, KrakenHttpClient kraken, List<LedgerEntry> entries) 
    throws KrakenException, SQLException {
    PreparedStatement st = db.prepareStatement("""
insert into
  kraken.entry_usd_reference_price
(
  entry_id,
  currency_pair,
  currency_price,
  currency_volume,
  currency_timestamp
) values (
  ?,
  ?,
  ?,
  ?,
  ?::text::timestamptz
) on conflict (entry_id) do nothing;
    """);

    for (LedgerEntry e : entries) {
      if (e.asset().startsWith("XETH")) {
        var ethusd = getTrades(kraken, "XETHZUSD", e.time(), e.time().minus(3, ChronoUnit.MINUTES));
        var ethusdTrade = ethusd.
          stream().
          reduce(ethusd.get(0), (latest, trade) -> {
            if (trade.time().compareTo(e.time()) <= 0)
              return trade;
            else
              return latest;
          });
        
        st.setString(1, e.entryId());
        st.setString(2, ethusdTrade.pair());
        st.setBigDecimal(3, ethusdTrade.price());
        st.setBigDecimal(4, ethusdTrade.volume());
        st.setString(5, ethusdTrade.time().toString());
        st.addBatch();
      } else if (e.asset().startsWith("XXBT")) {
        var btcusd = getTrades(kraken, "XXBTZUSD", e.time(), e.time().minus(1, ChronoUnit.MINUTES));
        var btcusdTrade = btcusd.
          stream().
          reduce(btcusd.get(0), (latest, trade) -> {
            if (trade.time().compareTo(e.time()) <= 0)
              return trade;
            else
              return latest;
          });

        st.setString(1, e.entryId());
        st.setString(2, btcusdTrade.pair());
        st.setBigDecimal(3, btcusdTrade.price());
        st.setBigDecimal(4, btcusdTrade.volume());
        st.setString(5, btcusdTrade.time().toString());
        st.addBatch();
      }
    }

    st.executeBatch();

    println("Saved prices for " + entries.size() + " ledger entries");
  }

  public static void main(String[] args) {
    var kraken = new KrakenHttpClient(apiKey, apiSecret);
    var start = Instant.now().minus(4, ChronoUnit.DAYS);
    var end = Instant.now();

    try {
      var db = DriverManager.getConnection(dbStr);

      var entries = kraken.getLedgersInfo(new GetLedgersInfo(start, end));

      persistLedgers(db, entries);

      persistReferencePrices(db, kraken, entries);

      // getLedgersInfo will only return 50 entries max; attempt to get the rest here
      while (entries.size() == 50) {
        try {
          Thread.sleep(5000);
        } catch (InterruptedException e) {
          println("Sleep interrupted");
        }

        var earliest = entries.stream().map(LedgerEntry::time).min(Instant::compareTo).get();

        println("earliest: " + earliest);

        entries = kraken.getLedgersInfo(new GetLedgersInfo(start, earliest));

        persistLedgers(db, entries);

        persistReferencePrices(db, kraken, entries);
      }
    } catch (KrakenException | SQLException e) {
      e.printStackTrace();
    }
  }
}
