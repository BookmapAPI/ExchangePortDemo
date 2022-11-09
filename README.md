# ExchangePort documentation & code examples

**Note: This project has been deprecated and is not accessible to users.**

ExchangePort is a Java library that provides programmatic access to a multitude of exchanges, using a singular
interface. The aim of the library is to lift developer's / quant's burden of doing the infrastructure work and make it
possible to focus purely on the trading strategy.

In this documentation we will start exploring ExchangePort, learning how to connect to the exchanges, subscribe to
order book and trades data, and how to place / modify / cancel orders.

Get the library now at [ExchangePort - Bookmap Marketplace](https://marketplace.bookmap.com/product/exchangeport/).

How to use the library? See [Usage](#Usage) below, and code examples in [src/main/java](src/main/java).

![ExchangePort logo](logo.png "ExchangePort Logo")

All currently available exchanges:

| Exchange        | Method name in *ConnectivityClient* |
| --------------- | ----------------------------------- |
| Binance Spot    | `binanceSpot`                       |
| Binance Futures | `binanceFutures`                    |
| Bitfinex        | `bitfinex`                          |
| bitFlyer        | `bitflyer`                          |
| bitstamp        | `bitstamp`                          |
| Bittrex         | `bittrex`                           |
| Bybit           | `bybit`                             |
| CoinFLEX        | `coinflex`                          |
| Deribit         | `deribit`                           |
| FTX             | `ftx`                               |
| HitBTC          | `hitBtc`                            |
| Huobi           | `huobi`                             |
| Kraken Spot     | `krakenSpot`                        |
| Kraken Futures  | `krakenFutures`                     |
| Poloniex        | `poloniex`                          |

# Usage

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
| **Provider** | An abstraction representing an exchange (e.g. FTX, Bitstamp), or a subset of an exchange (e.g. Binance Futures, Kraken Spot). Usually we use the term "exchange" and "provider" interchangeably. |
| **Instrument** | A financial instrument on a certain exchange (e.g. *BTCUSD*, *ETHUSD_PERP*). |
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
| **Price** | The price of the order/trade, as  floating point (e.g. 19,000.00 for *BTCUSD*). |
| **Price level** | The Price converted to an integer using the formula: `Price / Selected pip` (e.g. 1,900,000 if selected pip is 0.01). Used to avoid floating point imprecision. |
| **Size** | The size of the order/trade, as a floating point (e.g. 3 for *BTCUSD*). |
| **Size level** | The Size converted to an integer using the formula: `Size / Selected size increment` (e.g. 3000 if selected size increment is 0.001). Used to avoid floating point imprecision. |
| **Order duration** | How long the order will stay in the market (e.g. GTC - good-till-cancel, DAY  - today only, IOC immediate-or-cancel, etc.) |
| **Supported features** | Available functionality supported by the provider/exchange (contains e.g. a list of available order durations, whether it supports OCO, bracket orders, etc.) |
| **Token** | The string value you use to unlock the library. |


## Quick start

1. Construct the `ConnectivityClient` instance by providing a valid token (gotten on purchase).

    ```java
    ConnectivityClient client = new ConnectivityClient("YOUR_TOKEN");
    ```

2. Construct the [provider](#Glossary) you want to connect to.

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
   
    Overall connection to the exchange, login status, and any error messages are received using the *admin adapter listener*.

    ```java
    provider.addListener(new Layer1ApiAdminAdapter() {
        @Override public void onLoginFailed(LoginFailedReason reason, String message) {}
        @Override public void onLoginSuccessful() {}
        @Override public void onConnectionLost(DisconnectionReason reason, String message) {}
        @Override public void onConnectionRestored() {}
        @Override public void onSystemTextMessage(String message, SystemTextMessageType messageType) {}
        @Override public void onUserMessage(Object data) {}
    });
    ```

    Callbacks regarding [instrument subscriptions](#Glossary) are received using the *instrument listener*.

    ```java
    provider.addListener(new Layer1ApiInstrumentListener() {
        @Override public void onInstrumentAdded(String alias, InstrumentInfo instrumentInfo) {}
        @Override public void onInstrumentRemoved(String alias) {}
        @Override public void onInstrumentNotFound(String symbol, String exchange, String type) {}
        @Override public void onInstrumentAlreadySubscribed(String symbol, String exchange, String type) {}
    });
    ```

4. Subscribe to an [instrument](#Glossary).

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

   Note: see below how to setup order listeners, and how to update / cancel orders.

For code examples, see the `*Example.java` files in [src/main/java](src/main/java).


## Documentation

### Intro

The ExchangePort library is asynchronous, meaning that after you call one of the available methods,
the responses are mostly received via callbacks.

The high-level overview of the usage of the library is the following:
1. Construct the `ConnectivityClient` object using your ExchangePort *token* by which you authenticate.
2. Connect to (and optionally authenticate with) one or several exchanges by calling the methods on
   the `ConnectivityClient` object.
3. Subscribe to one or several instruments, to start receiving market data (order book and trades).
4. Send / update / cancel orders.

What must also be done in addition to the above is the following:
- Get a list of instruments on an exchange. Different exchanges can use different namings for same instruments.
  Alternatively, find what the instrument's name directly in the exchange's trading platform.
- Find out what the minimum supported pip and size increments are of the instrument you are subscribing to. In other
  words, what are the price granularity and the size granularity).
- Find out what order types (LMT, MKT) and durations (e.g. good-till-cancel, fill-or-kill) the exchange provides.
- Setup listeners with which you get information from the exchange (e.g. order book data, your order fills), connection
  status, and instrument subscription info.

### List available instruments

Here we print all the available symbols a provider has, together with the minimum pip and size increment.

When subscribing, you will most likely want to specify the minimum pip and size increment. You do have the option to
specify a larger pip and size increment, and the library will consolidate the data.

```java
System.out.println(provider.getSource() + " has these available instruments: ");
System.out.println();
System.out.printf("%20s | %20s | %20s%n", "Symbol", "Min pip", "Min size increment");
System.out.println("-----------------------------------------------------------");
for (SubscribeInfo instrument : provider.getSupportedFeatures().knownInstruments) {
   // Note: Most providers (but not all) use only symbol to identify an instrument, with the 'exchange' and
   // 'type' fields being empty/null.

   DefaultAndList<Double> pipsInfo = provider.getSupportedFeatures().pipsFunction.apply(instrument);
   double minimumPip = pipsInfo.valueOptions.get(0);

   DefaultAndList<Double> sizeMultiplierInfo = provider.getSupportedFeatures().sizeMultiplierFunction.apply(instrument);
   // Size multiplier is simply an inverse of the size increment.
   double maximumSizeMultiplier = sizeMultiplierInfo.valueOptions.get(0);
   double minimumSizeIncrement = 1 / maximumSizeMultiplier;

   System.out.printf("%20s | %20.8f | %20.8f%n", instrument.symbol, minimumPip, minimumSizeIncrement);
}
```

### List supported order durations and types

```java
System.out.println("Supported limit order durations: " + provider.getSupportedFeatures().supportedLimitDurations);
System.out.println("Supported stop order durations: " + provider.getSupportedFeatures().supportedStopDurations);
System.out.println("Supported stop order types: " + provider.getSupportedFeatures().supportedStopOrders);
```

Note: Limit types are always `LMT` and `STP`. If the exchange doesn't support market orders, you can still place it,
and we will send a limit order with a price 10% above/below the best ask/bid for a buy/sell order.

### Listeners

**Data listener**

Receive callbacks when the order book changes, or a trade happens.

```java
provider.addListener(
    new Layer1ApiDataListener() {
        @Override
        public void onTrade(String alias, double priceLevel, int sizeLevel, TradeInfo tradeInfo) {
            // Whether the trade aggressor is the buyer or the seller.
            boolean isBuy = tradeInfo.isBidAggressor;

            // 'priceLevel' is the actual price divided by your selected pip. For instance, if your pip is 0.1, and
            // the trade happened at price 123.4, the value of the 'priceLevel' variable will be 1234. To convert it
            // to the actual price, multiply it by your pip.
            // Note: The reason that 'priceLevel' is a double, not an integer, is because if your selected pip is
            // larger than the minimum pip, 'priceLevel' is a floating point. For instance, in the example above,
            // if you selected a pip of 10, the 'priceLevel' here would have a value of 12.34.
            double price = priceLevel * mySelectedPip;


           // 'sizeLevel' is the actual size divided by your selected size increment. For instance, if your size
           // increment is 0.1, and the trade's size is size 32.1, the value of the 'sizeLevel' variable will be 321.
           // To convert it to the actual size, multiply it by your size increment.
           double size = sizeLevel * mySelectedSizeIncrement;
        }

        @Override
        public void onDepth(String alias, boolean isBid, int priceLevel, int sizeLevel) {
            String side = isBid ? "BID" : "ASK";
            // See above the explanations for 'priceLevel' and 'sizeLevel', they apply here as well. The difference is
            // that 'priceLevel' is an integer. This means that if you select a pip that is larger than the minimum,
            // the depth price will be rounded up/down for an ask/bid order.
            double price = priceLevel * mySelectedPip;
            double size = sizeLevel * mySelectedSizeIncrement;
        }

        @Override
        public void onMarketMode(String alias, MarketMode marketMode) {
            // Not used in blockchain exchanges.
        }
    }
);
```

**Trading listener**

Receive callbacks about your own orders, account status, and balance.

```java
provider.addListener(
    new Layer1ApiTradingListener() {
        @Override
        public void onOrderUpdated(OrderInfoUpdate orderInfoUpdate) {
            // Called when your order has been placed/modified/canceled.
            System.out.println(orderInfoUpdate);
            switch (orderInfoUpdate.status) {
                case WORKING: {
                    workingOrders.put(orderInfoUpdate.orderId, orderInfoUpdate);
                    break;
                }
                case FILLED:
                case CANCELLED: {
                    workingOrders.remove(orderInfoUpdate.orderId);
                    break;
                }
            }
        }

        @Override
        public void onOrderExecuted(ExecutionInfo executionInfo) {
            // Called when an order has been (partially) executed.
            System.out.println(executionInfo);
        }

        @Override
        public void onStatus(StatusInfo statusInfo) {
            // Called when instrument status changes in some way - frequency depends on the exchange, but it usually 
            // immediately follows the change taking place.
            // `statusInfo` contains the following data:
            // - alias
            // - instrument's P&L, realized and unrealized (not available with every exchange)
            // - position
            // - average entry price
            // - volume
            // - working number of buys (summed buy order sizes)
            // - working number of sells (summed sell order sizes)
            System.out.println(statusInfo);
        }

        @Override
        public void onBalance(BalanceInfo balanceInfo) {
            // Called when the balance changes - frequency depends on the exchange.
            // `balanceInfo` contains the following data:
            // - balance
            // - total P&L, realized and unrealized (not available with every exchange)
            // - currency
            // - rate to base - this is usually the ratio to USD, used to easily sum balances in different currencies
            System.out.println(balanceInfo);
        }
    }
);
```

**Admin listener**

```java
provider.addListener(
    new Layer1ApiAdminListener() {
        @Override
        public void onLoginFailed(LoginFailedReason loginFailedReason, String message) {
            // The login has failed.
        }

        @Override
        public void onLoginSuccessful() {
            // The login has been successful. Connection to the exchange is established.
        }

        @Override
        public void onConnectionLost(DisconnectionReason disconnectionReason, String message) {
            // Connection to the exchange has been lost. For instance, this is called when your internet goes down,
            // or the exchange API is unavailable.
        }

        @Override
        public void onConnectionRestored() {
            // The connection to the exchange as been reastablished.
        }

        @Override
        public void onSystemTextMessage(String message, SystemTextMessageType systemTextMessageType) {
            // This is called when the provider informs the user about something, usually if something goes wrong.
        }

        @Override
        public void onUserMessage(Object o) {
            // Not used here. This can be ignored.
        }
    }
);
```

**MBO data listener**

Some exchanges provide MBO (market-by-order) order book data, telling you information about all orders, not just
price aggregation. Currently, no exchanges in this library support this (since blockchain exchanges don't provide this
data) but in the future, when we do have such exchanges, we'll document the listener behavior here.

### Trading

Here are the methods to manipulate your own orders.

**Place a new order**

Setting the limit and stop price determines the type. Specifically:
- If both limit and stop price are `Double.NaN`, the order will be MKT.
- If limit is not `Double.NaN` and stop price is `Double.NaN`, the order will be LMT.
- If limit is `Double.NaN` and stop price is not `Double.NaN`, the order will be STP.
- If both limit and stop price are not `Double.NaN`, the order will be STP\_LMT.

```java
provider.sendOrder(
    new SimpleOrderSendParameters(
	"BTC-USD",         // The alias.
	true,              // Whether it's a buy (true) or a sell (false) order.
	sizeLevel,         // The size level of the order (size level is equal to size * selectedSizeIncrement)
	OrderDuration.GTC, // The order duration (in this case GTC means good-till-cancel).
	"MY_ORDER_1",      // An ID you can assign to an order. Not all exchanges support this.
	limitPrice,        // The limit price.
	Double.NaN,        // The stop price.
	0,                 // Take profit offset. Used with bracket orders. If set to 0 it is ignored. Not supported by all exchanges.
	0,                 // Stop loss offset. Used with bracket orders. If set to 0 it is ignored. Not supported by all exchanges.
	0,                 // Trailing stop offset. Used with trailing stop orders. If set to 0 it is ignored. Not supported by all exchanges.
	0,                 // Trailing step. Used with trailing stop orders. If set to 0 it is ignored. Not supported by all exchanges.
	false              // If true, allow only reducing the position, not increasing it. Currently not supported.
    )
);
```

**Modify order's price**

```java
provider.updateOrder(
    new OrderMoveParameters(
        orderId,      // The order ID.
        Double.NaN,   // The new stop price. Set it to Double.NaN if the order's type is not STP or STP_LMT.
        newLimitPrice // The new limit price. Set it to Double.NaN if the order's type is not LMT or STP_LMT.
    )
);
```

Note: order's type cannot be changed. Meaning that, for example, if the order is LMT, you can only change its limit price
(the stop price should be, in this case, set to `Double.NaN`).

**Modify order's size**
```java
provider.updateOrder(
    new OrderResizeParameters(
        orderId, // The order ID.
        newSize  // The new size.
    )
);
```

**Cancel order**
```java
provider.updateOrder(
    new OrderCancelParameters(
        orderId // The order ID.
    )
);
```
