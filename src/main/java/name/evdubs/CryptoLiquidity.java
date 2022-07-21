package name.evdubs;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import name.evdubs.PreferredPrices.HigherLower;
import name.evdubs.req.AddOrder;
import name.evdubs.req.CancelOrder;
import name.evdubs.req.GetAccountBalance;
import name.evdubs.req.GetOpenOrders;
import name.evdubs.req.GetOrderBook;
import name.evdubs.rsp.OpenOrder;
import name.evdubs.rsp.OrderAction;
import name.evdubs.rsp.OrderBookEntry;

public class CryptoLiquidity 
{ 
  static MathContext mc3f = new MathContext(3, RoundingMode.FLOOR);

  static String apiKey = System.getenv("API_KEY");

  static String apiSecret = System.getenv("API_SECRET");

  static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX").withZone(ZoneId.of("Z"));

  static BigDecimal maxOrders = new BigDecimal(20);

  static BigDecimal lowMargin = new BigDecimal("0.975");

  static BigDecimal highMargin = new BigDecimal("1.025");

  static void computeAndSendOrders(KrakenHttpClient client, List<OpenOrder> orders, String pair, OrderAction action, 
    BigDecimal totalAmount, BigDecimal orderAmount) {

    var refPrice = client.getOrderBook(new GetOrderBook(pair)).
      get(action).
      get(0).
      price();

    var desiredPrices = action == OrderAction.BUY ? 
      PreferredPrices.getPrices(refPrice.multiply(lowMargin), HigherLower.LOWER).descendingSet() :
      PreferredPrices.getPrices(refPrice.multiply(highMargin), HigherLower.HIGHER);

    var filteredPrices = desiredPrices.stream().toList().subList(0, 
      totalAmount.divide(orderAmount, mc3f).intValue());

    var activeOrderPrices = orders.stream().
      filter(o -> o.assetPair().equals(pair) && o.action() == action).
      map(o -> o.price()).
      collect(Collectors.toCollection(TreeSet::new));
    
    var remainingPrices = filteredPrices.stream().
      filter(p -> !activeOrderPrices.contains(p)).
      toList();

    var newOrders = remainingPrices.stream().
      map(p -> new AddOrder("limit", pair, p, action.toString().toLowerCase(), 
        orderAmount.divide(p, mc3f))).
      toList();

    // print some useful info
    println("Called computeAndSendOrders with (" + pair + ", " + action + ", " + 
      totalAmount + ", " + orderAmount + ")");

    println("lowRefPrice: " + refPrice.multiply(lowMargin));
    println("highRefPrice: " + refPrice.multiply(highMargin));

    println("filteredPrices: " + filteredPrices);
    println("remainingPrices: " + remainingPrices);

    println("new " + pair + " orders: " + newOrders);
    println("new orders value " + newOrders.stream().
      map(o -> o.price().multiply(o.volume())).
      reduce(new BigDecimal(0), (acc, total) -> acc.add(total)));
    
    newOrders.forEach(o -> client.addOrder(o));
  }

  static BigDecimal midpoint(Map<OrderAction, List<OrderBookEntry>> book) {
    return book.get(OrderAction.BUY).get(0).price().
      add(book.get(OrderAction.SELL).get(0).price()).
      divide(new BigDecimal(2));
  }

  static void println(String s) {
    System.out.println(dtf.format(Instant.now()) + " " + s);
  }

  public static void main( String[] args )
  {
    var client = new KrakenHttpClient(apiKey, apiSecret);

    while(true) {
      var balance = client.getAccountBalance(new GetAccountBalance());

      var usdBalance = balance.getBalance("ZUSD");
      var btcBalance = balance.getBalance("XXBT");
      var ethBalance = balance.getBalance("XETH");

      var btcUsdBook = client.getOrderBook(new GetOrderBook("XBTUSD"));
      var ethUsdBook = client.getOrderBook(new GetOrderBook("ETHUSD"));

      var btcUsdPrice = midpoint(btcUsdBook);
      var ethUsdPrice = midpoint(ethUsdBook);

      // use max() here to make sure we can't end up with 0 values for the division later
      var btcBalanceUsd = btcBalance.multiply(btcUsdPrice, mc3f).max(BigDecimal.ONE);
      var ethBalanceUsd = ethBalance.multiply(ethUsdPrice, mc3f).max(BigDecimal.ONE);
      var totalBalanceUsd = usdBalance.add(btcBalanceUsd).add(ethBalanceUsd);

      var targetOrderValueUsd = totalBalanceUsd.divide(maxOrders, mc3f);
      
      var orders = client.getOpenOrders(new GetOpenOrders());

      BigDecimal usdOrderTotal = orders.stream().
        filter(o -> o.assetPair().contains("USD")).
        map(o -> o.price().multiply(o.volume())).
        reduce(BigDecimal.ZERO, (acc, orderTotal) -> acc.add(orderTotal));

      var realUsdRemaining = usdBalance.subtract(usdOrderTotal);

      // BTC USD

      var targetBtcUsdBuy = ethBalanceUsd.divide(btcBalanceUsd.add(ethBalanceUsd, mc3f), mc3f).
        multiply(usdBalance, mc3f);

      BigDecimal btcUsdOrderTotal = orders.stream().
        filter(o -> o.assetPair().contains("XBTUSD")).
        map(o -> o.price().multiply(o.volume())).
        reduce(BigDecimal.ZERO, (acc, orderTotal) -> acc.add(orderTotal));

      var btcUsdRemaining = targetBtcUsdBuy.subtract(btcUsdOrderTotal).min(realUsdRemaining);

      if (btcUsdRemaining.compareTo(targetOrderValueUsd) > 0) {
        println("BTC USD: buy " + btcUsdRemaining + " " + targetOrderValueUsd);
        computeAndSendOrders(client, orders, "XBTUSD", OrderAction.BUY, btcUsdRemaining, targetOrderValueUsd);
      }

      // ETH USD
      
      var targetEthUsdBuy = btcBalanceUsd.divide(btcBalanceUsd.add(ethBalanceUsd, mc3f), mc3f).
        multiply(usdBalance, mc3f);

      BigDecimal ethUsdOrderTotal = orders.stream().
        filter(o -> o.assetPair().contains("ETHUSD")).
        map(o -> o.price().multiply(o.volume())).
        reduce(BigDecimal.ZERO, (acc, orderTotal) -> acc.add(orderTotal));

      var ethUsdRemaining = targetEthUsdBuy.subtract(ethUsdOrderTotal).min(realUsdRemaining);

      if (ethUsdRemaining.compareTo(targetOrderValueUsd) > 0) {
        println("ETH USD: buy " + ethUsdRemaining + " " + targetOrderValueUsd);
        computeAndSendOrders(client, orders, "ETHUSD", OrderAction.BUY, ethUsdRemaining, targetOrderValueUsd);
      }

      // ETH BTC

      BigDecimal btcOrderTotal = orders.stream().
        filter(o -> o.assetPair().equals("ETHXBT") && o.action() == OrderAction.BUY).
        map(o -> o.price().multiply(o.volume())).
        reduce(BigDecimal.ZERO, (acc, orderTotal) -> acc.add(orderTotal));

      var btcRemaining = btcBalance.subtract(btcOrderTotal);

      var targetOrderValueBtc = targetOrderValueUsd.divide(btcUsdPrice, mc3f);

      if (btcRemaining.compareTo(targetOrderValueBtc) > 0) {
        println("ETH BTC: buy " + btcRemaining + " " + targetOrderValueBtc);
        computeAndSendOrders(client, orders, "ETHXBT", OrderAction.BUY, btcRemaining, targetOrderValueBtc);
      }

      BigDecimal ethOrderTotal = orders.stream().
        filter(o -> o.assetPair().equals("ETHXBT") && o.action() == OrderAction.SELL).
        map(o -> o.volume()).
        reduce(BigDecimal.ZERO, (acc, orderTotal) -> acc.add(orderTotal));

      var ethRemaining = ethBalance.subtract(ethOrderTotal);

      var targetOrderValueEth = targetOrderValueUsd.divide(ethUsdPrice, mc3f);

      if (ethRemaining.compareTo(targetOrderValueEth) > 0) {
        var ethBtcBook = client.getOrderBook(new GetOrderBook("ETHXBT"));
        var ethBtcPrice = midpoint(ethBtcBook);

        println("ETH BTC: sell " + ethRemaining.multiply(ethBtcPrice) + 
          " " + targetOrderValueBtc);
        computeAndSendOrders(client, orders, "ETHXBT", OrderAction.SELL, 
          ethRemaining.multiply(ethBtcPrice, mc3f), targetOrderValueBtc);
      }

      var time = Instant.now();

      // Clean up outstanding USD orders when ETH BTC trades happen or when we move to the next day
      if (btcUsdRemaining.compareTo(BigDecimal.ZERO) < 0 || ethUsdRemaining.compareTo(BigDecimal.ZERO) < 0 ||
        (time.atOffset(ZoneOffset.UTC).getHour() == 0 && time.atOffset(ZoneOffset.UTC).getMinute() == 0 && 
         time.atOffset(ZoneOffset.UTC).getSecond() < 30)) {
        println("Refreshing USD orders");
        orders.stream().
          filter(o -> o.assetPair().contains("USD")).
          forEach(o -> client.cancelOrder(new CancelOrder(o.transactionId())));
      }

      if (time.atOffset(ZoneOffset.UTC).getMinute() == 0 && time.atOffset(ZoneOffset.UTC).getSecond() < 30) {
        System.out.println(dtf.format(Instant.now()));
        System.out.println(
          "\tusdBalance:\t\t" + usdBalance + 
          "\n\tbtcBalance:\t\t" + btcBalance + 
          "\n\tethBalance:\t\t" + ethBalance +
          "\n\tbtcBalanceUsd:\t\t" + btcBalanceUsd + 
          "\n\tethBalanceUsd:\t\t" + ethBalanceUsd +
          "\n\ttotalBalanceUsd:\t" + totalBalanceUsd + 
          "\n\ttargetOrderValueUsd:\t" + targetOrderValueUsd + 
          "\n\tusdOrderTotal:\t\t" + usdOrderTotal + 
          "\n\trealUsdRemaining:\t" + realUsdRemaining + 
          "\n\ttargetBtcUsdBuy:\t" + targetBtcUsdBuy + 
          "\n\tbtcUsdOrderTotal:\t" + btcUsdOrderTotal + 
          "\n\tbtcUsdRemaining:\t" + btcUsdRemaining + 
          "\n\ttargetEthUsdBuy:\t" + targetEthUsdBuy + 
          "\n\tethUsdOrderTotal:\t" + ethUsdOrderTotal + 
          "\n\tethUsdRemaining:\t" + ethUsdRemaining + 
          "\n\ttargetOrderValueBtc:\t" + targetOrderValueBtc + 
          "\n\tbtcOrderTotal:\t\t" + btcOrderTotal + 
          "\n\tbtcRemaining:\t\t" + btcRemaining + 
          "\n\ttargetOrderValueEth:\t" + targetOrderValueEth + 
          "\n\tethOrderTotal:\t\t" + ethOrderTotal + 
          "\n\tethRemaining:\t\t" + ethRemaining
          );
      }

      try {
        Thread.sleep(30000, 0);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
