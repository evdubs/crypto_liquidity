package name.evdubs;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import name.evdubs.req.AddOrder;
import name.evdubs.req.CancelAll;
import name.evdubs.req.CancelOrder;
import name.evdubs.req.GetAccountBalance;
import name.evdubs.req.GetOpenOrders;
import name.evdubs.req.GetOrderBook;
import name.evdubs.req.GetTradesHistory;
import name.evdubs.req.GetWebSocketsToken;
import name.evdubs.req.HasPayload;
import name.evdubs.rsp.AccountBalance;
import name.evdubs.rsp.OpenOrder;
import name.evdubs.rsp.OrderAction;
import name.evdubs.rsp.OrderAdded;
import name.evdubs.rsp.OrderBookEntry;
import name.evdubs.rsp.OrdersCanceled;
import name.evdubs.rsp.Trade;
import name.evdubs.rsp.WebSocketsToken;

public class KrakenHttpClient {
  static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX").withZone(ZoneId.of("Z"));
  
  String apiKey = "";

  String apiSecret = "";

  public KrakenHttpClient(String apiKey, String apiSecret) {
    this.apiKey = apiKey;
    this.apiSecret = apiSecret;
  }

  <T extends HasPayload> String apiSign(T data)
      throws NoSuchAlgorithmException, InvalidKeyException {
    var sha256 = MessageDigest.getInstance("SHA-256");

    sha256.update(Long.toString(data.getNonce()).getBytes());

    sha256.update(data.getPayload().getBytes());

    var keySpec = new SecretKeySpec(Base64.getDecoder().decode(apiSecret), "HmacSHA512");
    var mac = Mac.getInstance("HmacSHA512");
    mac.init(keySpec);
    mac.update(data.getUrl().getBytes());
    mac.update(sha256.digest());

    return Base64.getEncoder().encodeToString(mac.doFinal()).trim();
  }

  <T extends HasPayload> JSONObject post(T data)
      throws InvalidKeyException, NoSuchAlgorithmException, IOException, InterruptedException, JSONException {
    var sign = apiSign(data);

    var client = HttpClient.newHttpClient();

    var request = HttpRequest.newBuilder().
      uri(URI.create("https://api.kraken.com" + data.getUrl())).
      timeout(Duration.ofSeconds(20)). // default timeout is infinity as of 2022-07-18
      header("API-Key", apiKey).header("API-Sign", sign).
      header("Content-Type", "application/x-www-form-urlencoded; charset=utf-8").
      POST(BodyPublishers.ofString(data.getPayload())).build();

    var response = client.send(request, BodyHandlers.ofString());

    return new JSONObject(response.body());
  }

  public OrderAdded addOrder(AddOrder req) throws KrakenException {
    var response = new JSONObject();

    try {
      response = post(req);
      System.out.println(dtf.format(Instant.now()) + " " + response.toString());
      return new OrderAdded(response.getJSONObject("result"));
    } catch (InvalidKeyException | NoSuchAlgorithmException | IOException | InterruptedException | JSONException e) {
      throw new KrakenException("addOrder response=" + response.toString(), e);
    }
  }

  public OrdersCanceled cancelAll() throws KrakenException {
    var response = new JSONObject();

    try {
      response = post(new CancelAll());
      return new OrdersCanceled(response.getJSONObject("result"));
    } catch (InvalidKeyException | NoSuchAlgorithmException | IOException | InterruptedException | JSONException e) {
      throw new KrakenException("cancelAll response=" + response.toString(), e);
    }
  }

  public OrdersCanceled cancelOrder(CancelOrder req) throws KrakenException {
    var response = new JSONObject();

    try {
      response = post(req);
      return new OrdersCanceled(response.getJSONObject("result"));
    } catch (InvalidKeyException | NoSuchAlgorithmException | IOException | InterruptedException | JSONException e) {
      throw new KrakenException("cancelOrder response=" + response.toString(), e);
    }
  }

  public AccountBalance getAccountBalance(GetAccountBalance req) throws KrakenException {
    var response = new JSONObject();

    try {
      response = post(req);
      return new AccountBalance(response);
    } catch (InvalidKeyException | NoSuchAlgorithmException | IOException | InterruptedException | JSONException e) {
      throw new KrakenException("getAccountBalance response=" + response.toString(), e);
    }
  }

  public List<OpenOrder> getOpenOrders(GetOpenOrders req) throws KrakenException {
    var response = new JSONObject();
    var orders = new ArrayList<OpenOrder>();

    try {
      response = post(req);
      JSONObject result = response.getJSONObject("result");
      for (String key : result.keySet()) {
        JSONObject status = result.getJSONObject(key);
        for (String transactionId : status.keySet()) {
          orders.add(new OpenOrder(transactionId, status.getJSONObject(transactionId)));
        }
      }
    } catch (InvalidKeyException | NoSuchAlgorithmException | IOException | InterruptedException | JSONException e) {
      throw new KrakenException("getOpenOrders response=" + response.toString(), e);
    }

    return orders;
  }

  public Map<OrderAction, List<OrderBookEntry>> getOrderBook(GetOrderBook req) throws KrakenException {
    var response = new JSONObject();
    var book = new HashMap<OrderAction, List<OrderBookEntry>>();

    try {
      response = post(req);
      var result = response.getJSONObject("result");
      for (String pair : result.keySet()) {
        var sides = result.getJSONObject(pair);
        for (String side : sides.keySet()) {
          var orders = sides.getJSONArray(side);
          var bookEntries = new ArrayList<OrderBookEntry>();
          orders.forEach(o -> bookEntries.add(new OrderBookEntry((JSONArray) o)));
          if ("bids".equals(side)) {
            book.put(OrderAction.BUY, bookEntries);
          } else {
            book.put(OrderAction.SELL, bookEntries);
          }
        }
      }
    } catch (InvalidKeyException | NoSuchAlgorithmException | IOException | InterruptedException | JSONException e) {
      throw new KrakenException("getOrderBook response=" + response.toString(), e);
    }

    return book;
  }

  public List<Trade> getTradesHistory(GetTradesHistory req) throws KrakenException {
    var response = new JSONObject();
    var tradesHistory = new ArrayList<Trade>();

    try {
      response = post(req);
      var result = response.getJSONObject("result");
      var trades = result.getJSONObject("trades");
      for (String transactionId : trades.keySet()) {
        var trade = trades.getJSONObject(transactionId);
        tradesHistory.add(new Trade(transactionId, trade));
      }
    } catch (InvalidKeyException | NoSuchAlgorithmException | JSONException | IOException | InterruptedException e) {
      throw new KrakenException("getTradesHistory response=" + response.toString(), e);
    }

    return tradesHistory;
  }

  public WebSocketsToken getWebSocketsToken(GetWebSocketsToken req) throws KrakenException {
    var response = new JSONObject();

    try {
      response = post(req);
      return new WebSocketsToken(post(req));
    } catch (InvalidKeyException | NoSuchAlgorithmException | IOException | InterruptedException | JSONException e) {
      throw new KrakenException("getWebSocketsToken response=" + response.toString(), e);
    }
  }
}
