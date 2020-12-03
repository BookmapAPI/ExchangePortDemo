# ExchangePort example

The ExchangePort library provides programmatic access to a multitude of exchanges, using a singular interface. The aim 
of the library is to lift developer's / quant's burden of doing the infrastructure work and make it possible to 
focus purely on the trading strategy.

Get the library now at [ExchangePort - Bookmap Marketplace](https://marketplace.bookmap.com/product/exchangeport/).

![ExchangePort logo](logo.png "ExchangePort Logo")

All currently available exchanges:
- `binanceSpot`
- `binanceFutures`
- `bitfinex`
- `bitflyer`
- `bitstamp`
- `bittrex`
- `bybit`
- `coinflex`
- `deribit`
- `ftx`
- `hitBtc`
- `huobi`
- `krakenSpot`
- `krakenFutures`
- `poloniex`

This repository shows how to use the library.

## Setup

Using Gradle, add your ExchangePort JAR. Also required is `org.apache.httpcomponents:httpclient`.

```
dependencies {
    // This assumes you have your ExchangePort JAR file in a the `libs` subdirectory.
    implementation fileTree(dir: 'libs', include: ['*.jar'])

    implementation group: 'org.apache.httpcomponents', name: 'httpclient', version: '4.5.2'
}
```


## Glossary

| Term | Definition |
|---|---|
| **Provider** | An abstraction representing an exchange (e.g. FTX, Bitstamp), or a subset of an exchange (e.g. Binance Futures, Kraken Futures). |
| **Instrument** | A financial instrument on a certain exchange (e.g. BTCUSD, ETHUSD_PERP). |
| **Symbol** | Name of the instrument. |
| **Type** | Type of the instrument. Currently mostly not used. |
| **Exchange** | Exchange of the instrument. Currently mostly not used. |
| **Alias** | *Extended* name of the instrument. Currently, it's mostly equal to Symbol, but it may contain the exchange name in the future. |
| **Subscription** | A connection to an Instrument. Causes market data (order book, trades) to be received. Enables trading. |
| **Listener** | A class containing callbacks to receive events from the exchange. |
| **Instrument pip** | The price granularity, or the smallest allowed price change allowed by the instrument. |
| **Selected pip** | The pip you select, should be at least Instrument pip. If larger, it aggregates smaller increments. |
| **Instrument size increment** | The size granularity, or the smallest allowed size change allowed by the instrument. |
| **Selected size increment** | The size increment you select, should be at least Instrument size increment. If larger, it aggregates the smaller increments. |
| **Price** | The price of the order/trade, as  floating point (e.g. 19,000.00 for BTCUSD). |
| **Price level** | The Price converted to an integer using the formula: `Price / Selected pip` (e.g. 1,900,000 if selected pip is 0.01). Used to avoid floating point imprecision. |
| **Size** | The size of the order/trade, as a floating point (e.g. 3 for BTCUSD). |
| **Size level** | The Size converted to an integer using the formula: `Size / Selected size increment` (e.g. 3000 if selected size increment is 0.001). Used to avoid floating point imprecision. |
| **Order duration** | How long the order will stay in the market (e.g. GTC - good-till-cancel, DAY  - today only, IOC immediate-or-cancel, etc.) |
| **Supported features** | Available functionality supported by the provider/exchange (contains e.g. a list of available order durations, whether it supports OCO, bracket orders, etc.) |
| **Token** | The string value you use to unlock the library. |


## Quick start

Find the `*Example.java` files in this repository showing usage in several differnt ways.

Also, here's a short recap:

1. Construct the `ConnectivityClient` instance by providing a valid token (gotten on purchase).

    ```java
    ConnectivityClient client = new ConnectivityClient("YOUR_TOKEN");
    ```

2. Construct the provider you want to connect to.

   Here's are two ways to connect to the FTX exchange.

    ```java
    // Connect as "read-only", to be able to observe the order book and market trades, but not to place orders.
    Layer1ApiProvider provider = client.ftx();
   
    // Connect with your FTX keys to be able to place and manipulate your orders, observe positions etc.
    Layer1ApiProvider provider = client.ftx("YOUR_FTX_API_KEY", "YOUR_FTX_API_SECRET");
    ```

   > Note: In the nomenclature of this library, the class by which you connect to an exchange is called a "provider".

3. Setup listeners.

    Add your listeners to observe what's going on in the market.

    The market data is received using the *data listener*.

    ```java
    provider.addListener(new Layer1ApiDataListener() {
        /**
         * Called when a trade happens in the market.
         */
        @Override public void onTrade(String alias, double priceLevel, int sizeLevel, TradeInfo tradeInfo) {}
        /**
         * Called when the order book is updated.
         */
        @Override public void onDepth(String alias, boolean isBid, int priceLevel, int sizeLevel) {}
        /**
         * Not used in blockchain exchanges.
         */
        @Override public void onMarketMode(String s, MarketMode marketMode) {} 
    });
    ```
   
    Connections and 

    ```java
    provider.addListener(new Layer1ApiAdminAdapter() {
        @Override public void onLoginFailed(LoginFailedReason reason, String message) {}
        @Override public void onLoginSuccessful() {}
        @Override public void onConnectionLost(DisconnectionReason reason, String message) {}
        @Override public void onConnectionRestored() {}
        @Override public void onSystemTextMessage(String message, SystemTextMessageType messageType) {}
        @Override public void onUserMessage(Object data) {}
    });
    provider.addListener(new Layer1ApiInstrumentListener() {
        @Override public void onInstrumentAdded(String alias, InstrumentInfo instrumentInfo) {}
        @Override public void onInstrumentRemoved(String s) {}
        @Override public void onInstrumentNotFound(String s, String s1, String s2) {}
        @Override public void onInstrumentAlreadySubscribed(String s, String s1, String s2) {}
    });
    ```

4. Subscribe to an instrument.

    ```java
    String symbol = "ETH/USD";
    double pip = 0.01;
    double sizeMultiplier = 1;
    provider.subscribe(new SubscribeInfoCrypto(symbol, null, null, pip, sizeMultiplier));
    ```

   Before proceeding, wait for one of the `Layer1ApiInstrumentListener` method to be called.

5. Place an order (only if provider was constructed with credentials).

    ```java
    boolean isBuy = true;
    int size = 1;
    OrderDuration duration = OrderDuration.GTC;
    double limitPrice = 370.0;
    double stopPrice = Double.NaN;
    provider.sendOrder(new SimpleOrderSendParameters(symbol, isBuy, size, duration, limitPrice, stopPrice));
    ```
