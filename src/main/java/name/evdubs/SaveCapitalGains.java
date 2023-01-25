package name.evdubs;

import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import name.evdubs.rsp.OrderAction;

public class SaveCapitalGains {
  static String dbStr = System.getenv("DB");

  static MathContext mc8 = new MathContext(8);

  record UsdTrade(
    String transactionId,
    String symbol,
    Instant time,
    OrderAction action,
    BigDecimal price,
    BigDecimal amount,
    BigDecimal fee,
    BigDecimal cost
  ) {}

  record CapitalGainLoss(
    String symbol,
    Instant acquireTime,
    String acquireTransactionId,
    BigDecimal costBasis,
    Instant disposeTime,
    String disposeTransactionId,
    BigDecimal proceeds,
    BigDecimal gainLoss
  ) {}

  private static void saveGainLoss(Connection db, List<UsdTrade> trades) {
    var buys = trades.stream().filter(t -> t.action() == OrderAction.BUY).collect(Collectors.toList());

    var sells = trades.stream().filter(t -> t.action() == OrderAction.SELL).collect(Collectors.toList());

    UsdTrade currentBuy = null;
    UsdTrade currentSell = null;

    var gainLoss = new ArrayList<CapitalGainLoss>();

    while (!sells.isEmpty()) {
      if (currentBuy == null) {
        currentBuy = buys.get(0);
        buys.remove(0);
      }
        
      if (currentSell == null) {
        currentSell = sells.get(0);
        sells.remove(0);
      }
      
      if (currentBuy.amount().compareTo(currentSell.amount()) == 0) {
        gainLoss.add(new CapitalGainLoss(
          currentBuy.symbol(), 
          currentBuy.time(), 
          currentBuy.transactionId(), 
          currentBuy.price().multiply(currentBuy.amount(), mc8), 
          currentSell.time(), 
          currentSell.transactionId(), 
          currentSell.price().multiply(currentSell.amount(), mc8), 
          currentSell.price().multiply(currentSell.amount()).subtract(
            currentBuy.price().multiply(currentBuy.amount()), mc8
          )));

        currentBuy = null;
        currentSell = null;
      } else if (currentBuy.amount().compareTo(currentSell.amount()) > 0) {
        gainLoss.add(new CapitalGainLoss(
          currentBuy.symbol(), 
          currentBuy.time(), 
          currentBuy.transactionId(), 
          currentBuy.price().multiply(currentSell.amount(), mc8), 
          currentSell.time(), 
          currentSell.transactionId(), 
          currentSell.price().multiply(currentSell.amount(), mc8), 
          currentSell.price().multiply(currentSell.amount()).subtract(
            currentBuy.price().multiply(currentSell.amount()), mc8
          )));

        currentBuy = new UsdTrade(
          currentBuy.transactionId(),
          currentBuy.symbol(),
          currentBuy.time(),
          currentBuy.action(),
          currentBuy.price(),
          currentBuy.amount().subtract(currentSell.amount(), mc8),
          currentBuy.fee().multiply(currentBuy.amount().subtract(currentSell.amount(), mc8))
            .divide(currentBuy.amount(), mc8),
          currentBuy.price().multiply(currentBuy.amount().subtract(currentSell.amount()), mc8));
        
        currentSell = null;
      } else if (currentBuy.amount().compareTo(currentSell.amount()) < 0) {
        gainLoss.add(new CapitalGainLoss(
          currentBuy.symbol(), 
          currentBuy.time(), 
          currentBuy.transactionId(), 
          currentBuy.price().multiply(currentBuy.amount(), mc8), 
          currentSell.time(), 
          currentSell.transactionId(), 
          currentSell.price().multiply(currentBuy.amount(), mc8), 
          currentSell.price().multiply(currentBuy.amount()).subtract(
            currentBuy.price().multiply(currentBuy.amount()), mc8
          )));

        currentSell = new UsdTrade(
          currentSell.transactionId(),
          currentSell.symbol(),
          currentSell.time(),
          currentSell.action(),
          currentSell.price(),
          currentSell.amount().subtract(currentBuy.amount(), mc8),
          currentSell.fee().multiply(currentSell.amount().subtract(currentBuy.amount(), mc8))
            .divide(currentSell.amount(), mc8),
          currentSell.price().multiply(currentSell.amount().subtract(currentBuy.amount()), mc8));

        currentBuy = null;
      }
    }

    try {
      var ps = db.prepareStatement("""
insert into kraken.capital_gain_loss (
  symbol,
  acquire_timestamp,
  acquire_transaction_id,
  cost_basis,
  dispose_timestamp,
  dispose_transaction_id,
  proceeds,
  gain_loss
) values (
  ?,
  ?::text::timestamptz,
  ?,
  ?,
  ?::text::timestamptz,
  ?,
  ?,
  ?
);
          """);
      
      for (CapitalGainLoss gl : gainLoss) {
        ps.setString(1, gl.symbol());
        ps.setString(2, gl.acquireTime().toString());
        ps.setString(3, gl.acquireTransactionId());
        ps.setBigDecimal(4, gl.costBasis());
        ps.setString(5, gl.disposeTime().toString());
        ps.setString(6, gl.disposeTransactionId());
        ps.setBigDecimal(7, gl.proceeds());
        ps.setBigDecimal(8, gl.gainLoss());

        ps.addBatch();
      }

      ps.executeBatch();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    try {
      var db = DriverManager.getConnection(dbStr);

      var ps = db.prepareStatement("""
select 
  t.transaction_id,
  t.pair,
  to_char(t.\"timestamp\" at time zone 'UTC', 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"') as \"timestamp\",
  t.\"action\"::text as \"action\",
  t.price,
  t.\"cost\",
  t.fee,
  t.volume,
  urp.base_currency_price,
  urp.counter_currency_price
from 
  kraken.trade t
left outer join
  kraken.usd_reference_price urp 
on
  t.transaction_id = urp.transaction_id
order by
  t.\"timestamp\"
      """);

      var rs = ps.executeQuery();

      var btc = new ArrayList<UsdTrade>();

      var eth = new ArrayList<UsdTrade>();

      while (rs.next()) {
        var pair = rs.getString("pair");
        var base = pair.substring(0, 4);
        var counter = pair.substring(4, 8);

        if ("XXBTZUSD".equals(pair)) {
          btc.add(new UsdTrade(
            rs.getString("transaction_id"), 
            base, 
            Instant.parse(rs.getString("timestamp")), 
            OrderAction.fromString(rs.getString("action")), 
            rs.getBigDecimal("price"), 
            rs.getBigDecimal("volume"), 
            rs.getBigDecimal("fee"), 
            rs.getBigDecimal("cost")));
        } else if ("XETHZUSD".equals(pair)) {
          eth.add(new UsdTrade(
            rs.getString("transaction_id"), 
            base, 
            Instant.parse(rs.getString("timestamp")), 
            OrderAction.fromString(rs.getString("action")), 
            rs.getBigDecimal("price"), 
            rs.getBigDecimal("volume"), 
            rs.getBigDecimal("fee"), 
            rs.getBigDecimal("cost")));
        } else if ("XETHXXBT".equals(pair)) {
          eth.add(new UsdTrade(
            rs.getString("transaction_id"),
            base, 
            Instant.parse(rs.getString("timestamp")), 
            OrderAction.fromString(rs.getString("action")), 
            rs.getBigDecimal("counter_currency_price").multiply(rs.getBigDecimal("price"), mc8), 
            rs.getBigDecimal("volume"), 
            rs.getBigDecimal("counter_currency_price").multiply(rs.getBigDecimal("fee")).divide(new BigDecimal(2), mc8), 
            rs.getBigDecimal("counter_currency_price").multiply(rs.getBigDecimal("price")).multiply(rs.getBigDecimal("volume"), mc8)));
          
          btc.add(new UsdTrade(
            rs.getString("transaction_id"),
            counter,
            Instant.parse(rs.getString("timestamp")),
            OrderAction.fromString(rs.getString("action")) == OrderAction.BUY ? OrderAction.SELL : OrderAction.BUY,
            rs.getBigDecimal("counter_currency_price"),
            rs.getBigDecimal("price").multiply(rs.getBigDecimal("volume"), mc8),
            rs.getBigDecimal("counter_currency_price").multiply(rs.getBigDecimal("fee")).divide(new BigDecimal(2), mc8),
            rs.getBigDecimal("counter_currency_price").multiply(rs.getBigDecimal("price")).multiply(rs.getBigDecimal("volume"), mc8)));
        }
      }

      saveGainLoss(db, btc);

      saveGainLoss(db, eth);

    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
