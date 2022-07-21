package name.evdubs;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class PreferredPrices {
  static MathContext mc3c = new MathContext(3, RoundingMode.CEILING);

  static MathContext mc3 = new MathContext(3);

  static MathContext mc3f = new MathContext(3, RoundingMode.FLOOR);

  static Double[] nums = 
    {.619, .634, .649, .665, .681, .698, .715, .732, .750, .768, 
     .787, .806, .825, .845, .866, .887, .909, .931, .953, .976,
     1.00, 1.02, 1.05, 1.07, 1.10, 1.13, 1.15, 1.18, 1.21, 1.24, 
     1.27, 1.30, 1.33, 1.37, 1.40, 1.43, 1.47, 1.50, 1.54, 1.58, 
     1.62, 1.65, 1.69, 1.74, 1.78, 1.82, 1.87, 1.91, 1.96, 2.00, 
     2.05, 2.10, 2.15, 2.21, 2.26, 2.32, 2.37, 2.43, 2.49, 2.55, 
     2.61, 2.67, 2.74, 2.80, 2.87, 2.94, 3.01, 3.09, 3.16, 3.24, 
     3.32, 3.40, 3.48, 3.57, 3.65, 3.74, 3.83, 3.92, 4.02, 4.12, 
     4.22, 4.32, 4.42, 4.53, 4.64, 4.75, 4.87, 4.99, 5.11, 5.23, 
     5.36, 5.49, 5.62, 5.76, 5.90, 6.04, 6.19, 6.34, 6.49, 6.65, 
     6.81, 6.98, 7.15, 7.32, 7.50, 7.68, 7.87, 8.06, 8.25, 8.45, 
     8.66, 8.87, 9.09, 9.31, 9.53, 9.76, 10.0, 10.2, 10.5, 10.7,
     11.0, 11.3, 11.5, 11.8, 12.1, 12.4, 12.7, 13.0, 13.3, 13.7, 
     14.0, 14.3, 14.7, 15.0, 15.4, 15.8, 16.2};
        
  static TreeSet<BigDecimal> numSet = new TreeSet<BigDecimal>(
    Arrays.
      asList(nums).
      stream().
      map(p -> new BigDecimal(p, mc3)).
      collect(Collectors.toCollection(ArrayList::new)));

  enum HigherLower {
    HIGHER,
    LOWER;
  }

  public static NavigableSet<BigDecimal> getPrices(BigDecimal price, HigherLower higherLower) {
    var doublePrice = price.doubleValue();
    var factor = Math.pow(10, (Math.floor(Math.log10(doublePrice))));

    if (higherLower == HigherLower.HIGHER) {
      var adjustedPrice = new BigDecimal(doublePrice / factor, mc3c);

      return numSet.
        subSet(adjustedPrice, false, adjustedPrice.multiply(new BigDecimal(5.0/4.0, mc3c)), true).
        stream().
        map(p -> p.multiply(new BigDecimal(factor), mc3c)).
        collect(Collectors.toCollection(TreeSet::new));
    } else {
      var adjustedPrice = new BigDecimal(doublePrice / factor, mc3f);

      return numSet.
        subSet(adjustedPrice.multiply(new BigDecimal(4.0/5.0, mc3f)), true, adjustedPrice, false).
        stream().
        map(p -> p.multiply(new BigDecimal(factor), mc3f)).
        collect(Collectors.toCollection(TreeSet::new));
    }
  }
}
