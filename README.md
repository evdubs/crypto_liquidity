CryptoLiquidity is a Java console application that allows Kraken account holders to send bids and offers for the ETH-BTC trading pair. The idea of this market maker is to accumulate ETH and BTC at first through the ETH-USD and BTC-USD markets via bids and then, once ETH and BTC tokens have been acquired, buy and sell them via ETH-BTC when one of these tokens is less favorable.

## Installation
CryptoLiquidity requires a JDK and Apache Maven.

## Starting CryptoLiquidity
After cloning the repository, execute the following commands in the `crypto_liquidity` base directory:

```bash
$ mvn compile
$ API_KEY="..." API_SECRET="..." mvn exec:java -Dexec.mainClass="name.evdubs.CryptoLiquidity"
```
