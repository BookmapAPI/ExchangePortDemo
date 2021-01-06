package com.bookmap.exchangeportexamples;

import com.bookmap.exchangeport.ConnectivityClient;
import java.util.HashMap;
import java.util.Map;
import velox.api.layer1.Layer1ApiDataListener;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.data.*;

public class MarketDataMultipleExchangesExample {

    private final ConnectivityClient client;

    public MarketDataMultipleExchangesExample() {
        client = new ConnectivityClient(Settings.EXCHANGEPORT_TOKEN);
    }

    public void run() {
        // Construct your chosen provider(s) (which is an abstraction of the exchange you want to connect to).
        // You may construct multiple various providers and use the same or different listeners
        // to listen to the data (see below).
        Layer1ApiProvider krakenFuturesProvider = client.krakenFutures(false);
        // Binance Futures API enables you to select the frequency of order book updates. At the time of this writing,
        // "100 milliseconds" is the highest available frequency (where order book updates are most frequent).
        Layer1ApiProvider binanceFuturesProvider = client.binanceFutures(false, "100 milliseconds");

        System.out.println("Available symbols in Kraken Futures:");
        printAvailableSymbols(krakenFuturesProvider);

        System.out.println("Available symbols in Binance Futures:");
        printAvailableSymbols(binanceFuturesProvider);

        // Let's say we are interested in the ETH / USD perpetual future instruments from these two exchanges. First,
        // we find the names: PI_ETHUSD on Kraken Futures, and ETHUSD_PERP on Binance Futures.

        // First, we must specify the pip and the size increment to which the data is going to be rounded.
        // (pip = price increment)
        //
        // The chosen values must be at least the minimum available exchange pip / size instrument
        // for the specified instrument.
        // For example: if BTCUSDT on Binance Futures has a minimum pip of 0.001, you can select 0.01 to
        // make all prices be rounded to 0.01, like it's done below.

        Map<String, Double> selectedPipAtKrakenFutures = new HashMap<String, Double>() {
            {
                put("PI_ETHUSD", 0.1);
            }
        };
        Map<String, Double> selectedSizeIncrementAtKrakenFutures = new HashMap<String, Double>() {
            {
                put("PI_ETHUSD", 0.001);
            }
        };

        Map<String, Double> selectedPipAtBinanceFutures = new HashMap<String, Double>() {
            {
                put("ETHUSD_PERP", 0.1);
            }
        };
        Map<String, Double> selectedSizeIncrementAtBinanceFutures = new HashMap<String, Double>() {
            {
                put("ETHUSD_PERP", 0.001);
            }
        };

        // Subscribe to the two instruments. After that, order book and trade data will start arriving.
        krakenFuturesProvider.subscribe(
            new SubscribeInfoCrypto(
                "PI_ETHUSD",
                null,
                null,
                selectedPipAtKrakenFutures.get("PI_ETHUSD"),
                1 / selectedSizeIncrementAtKrakenFutures.get("PI_ETHUSD")
            )
        );
        binanceFuturesProvider.subscribe(
            new SubscribeInfoCrypto(
                "ETHUSD_PERP",
                null,
                null,
                selectedPipAtBinanceFutures.get("ETHUSD_PERP"),
                1 / selectedSizeIncrementAtBinanceFutures.get("ETHUSD_PERP")
            )
        );

        // It's possible ot use the same listener for multiple providers. But the problem would occur when the alias
        // name is the same for multiple exchanges - meaning you wouldn't be able to know from which exchange the
        // event came from. That's why, below, two separate listeners are used (but you could safely use the same
        // listener here since the names PI_ETHUSD and ETHUSD_PERP aren't the same).

        krakenFuturesProvider.addListener(
            new Layer1ApiDataListener() {
                @Override
                public void onTrade(String alias, double priceLevel, int sizeLevel, TradeInfo tradeInfo) {
                    String buyOrSell = tradeInfo.isBidAggressor ? "BUY" : "SELL";
                    double price = priceLevel * selectedPipAtKrakenFutures.get(alias);
                    double size = sizeLevel * selectedSizeIncrementAtKrakenFutures.get(alias);
                    System.out.printf(
                        "onTrade: alias=%s buyOrSellAggressor=%s price=%s size=%s%n",
                        alias,
                        buyOrSell,
                        price,
                        size
                    );
                }

                @Override
                public void onDepth(String alias, boolean isBid, int priceLevel, int sizeLevel) {
                    String side = isBid ? "BID" : "ASK";
                    double price = priceLevel * selectedPipAtKrakenFutures.get(alias);
                    double size = sizeLevel * selectedSizeIncrementAtKrakenFutures.get(alias);
                    System.out.printf("onDepth: alias=%s side=%s price=%s size=%s %n", alias, side, price, size);
                }

                @Override
                public void onMarketMode(String s, MarketMode marketMode) {
                    // Not used in blockchain exchanges.
                }
            }
        );

        binanceFuturesProvider.addListener(
            new Layer1ApiDataListener() {
                @Override
                public void onTrade(String alias, double priceLevel, int sizeLevel, TradeInfo tradeInfo) {
                    String buyOrSell = tradeInfo.isBidAggressor ? "BUY" : "SELL";
                    double price = priceLevel * selectedPipAtBinanceFutures.get(alias);
                    double size = sizeLevel * selectedSizeIncrementAtBinanceFutures.get(alias);
                    System.out.printf(
                        "onTrade: alias=%s buyOrSellAggressor=%s price=%s size=%s%n",
                        alias,
                        buyOrSell,
                        price,
                        size
                    );
                }

                @Override
                public void onDepth(String alias, boolean isBid, int priceLevel, int sizeLevel) {
                    String side = isBid ? "BID" : "ASK";
                    double price = priceLevel * selectedPipAtBinanceFutures.get(alias);
                    double size = sizeLevel * selectedSizeIncrementAtBinanceFutures.get(alias);
                    System.out.printf("onDepth: alias=%s side=%s price=%s size=%s %n", alias, side, price, size);
                }

                @Override
                public void onMarketMode(String s, MarketMode marketMode) {
                    // Not used in blockchain exchanges.
                }
            }
        );
    }

    /**
     * List the available symbols for this provider.
     * Note that not all exchanges have the same nomenclature. One exchange might use the symbol
     * name BTCUSD, another XBTUSD etc.
     *
     * @param provider The provider (abstraction of an exchange).
     */
    private void printAvailableSymbols(Layer1ApiProvider provider) {
        for (SubscribeInfo subscribeInfo : provider.getSupportedFeatures().knownInstruments) {
            System.out.println("\t" + subscribeInfo.symbol);
        }
        System.out.println();
    }

    public static void main(String[] args) {
        new MarketDataMultipleExchangesExample().run();
    }
}
