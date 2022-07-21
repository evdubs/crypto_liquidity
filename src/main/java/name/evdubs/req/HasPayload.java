package name.evdubs.req;

public interface HasPayload extends HasNonce {
  public long getNonce();
  public String getPayload();
  public String getUrl();
}
