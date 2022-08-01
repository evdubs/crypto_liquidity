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
  static MathContext mc4c = new MathContext(4, RoundingMode.CEILING);

  static MathContext mc4 = new MathContext(4);

  static MathContext mc4f = new MathContext(4, RoundingMode.FLOOR);

  static Double[] nums = 
    {.6205, .6305, .6406, .6508, .6612, .6718, .6826, .6935, .7046, .7158,
     .7273, .7389, .7508, .7628, .7750, .7874, .8000, .8128, .8258, .8390,
     .8524, .8661, .8799, .8940, .9083, .9228, .9376, .9526, .9678, .9833,
     .9991, 1.016, 1.032, 1.049, 1.066, 1.083, 1.100, 1.118, 1.135, 1.154,
     1.172, 1.191, 1.210, 1.229, 1.249, 1.269, 1.289, 1.310, 1.331, 1.352,
     1.374, 1.396, 1.418, 1.441, 1.464, 1.487, 1.511, 1.535, 1.560, 1.585,
     1.610, 1.636, 1.662, 1.688, 1.715, 1.743, 1.771, 1.799, 1.828, 1.857,
     1.887, 1.917, 1.948, 1.979, 2.011, 2.043, 2.075, 2.109, 2.142, 2.177,
     2.211, 2.247, 2.283, 2.319, 2.356, 2.394, 2.432, 2.471, 2.511, 2.551,
     2.592, 2.633, 2.676, 2.718, 2.762, 2.806, 2.851, 2.897, 2.943, 2.990,
     3.038, 3.086, 3.136, 3.186, 3.237, 3.289, 3.341, 3.395, 3.449, 3.504,
     3.560, 3.617, 3.675, 3.734, 3.794, 3.854, 3.916, 3.979, 4.042, 4.107,
     4.173, 4.240, 4.307, 4.376, 4.446, 4.518, 4.590, 4.663, 4.738, 4.814,
     4.891, 4.969, 5.048, 5.129, 5.211, 5.295, 5.379, 5.465, 5.553, 5.642,
     5.732, 5.824, 5.917, 6.012, 6.108, 6.205, 6.305, 6.406, 6.508, 6.612,
     6.718, 6.826, 6.935, 7.046, 7.158, 7.273, 7.389, 7.508, 7.628, 7.750,
     7.874, 8.000, 8.128, 8.258, 8.390, 8.524, 8.661, 8.799, 8.940, 9.083,
     9.228, 9.376, 9.526, 9.678, 9.833, 9.991, 10.16, 10.32, 10.49, 10.66,
     10.83, 11.00, 11.18, 11.35, 11.54, 11.72, 11.91, 12.10, 12.29, 12.49,
     12.69, 12.89, 13.10, 13.31, 13.52, 13.74, 13.96, 14.18, 14.41, 14.64,
     14.87, 15.11, 15.35, 15.60, 15.85, 16.10};
        
  static TreeSet<BigDecimal> numSet = new TreeSet<BigDecimal>(
    Arrays.
      asList(nums).
      stream().
      map(p -> new BigDecimal(p, mc4)).
      collect(Collectors.toCollection(ArrayList::new)));

  enum HigherLower {
    HIGHER,
    LOWER;
  }

  public static NavigableSet<BigDecimal> getPrices(BigDecimal price, HigherLower higherLower) {
    var doublePrice = price.doubleValue();
    var factor = Math.pow(10, (Math.floor(Math.log10(doublePrice))));

    if (higherLower == HigherLower.HIGHER) {
      var adjustedPrice = new BigDecimal(doublePrice / factor, mc4c);

      return numSet.
        subSet(adjustedPrice, false, adjustedPrice.multiply(new BigDecimal(8.0/5.0, mc4c)), true).
        stream().
        map(p -> p.multiply(new BigDecimal(factor), mc4c)).
        collect(Collectors.toCollection(TreeSet::new));
    } else {
      var adjustedPrice = new BigDecimal(doublePrice / factor, mc4f);

      return numSet.
        subSet(adjustedPrice.multiply(new BigDecimal(5.0/8.0, mc4f)), true, adjustedPrice, false).
        stream().
        map(p -> p.multiply(new BigDecimal(factor), mc4f)).
        collect(Collectors.toCollection(TreeSet::new));
    }
  }
}
