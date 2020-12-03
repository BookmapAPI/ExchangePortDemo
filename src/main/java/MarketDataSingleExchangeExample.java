import com.bookmap.connectivitylib.ConnectivityClient;
import java.util.HashMap;
import java.util.Map;
import velox.api.layer1.Layer1ApiDataListener;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.data.MarketMode;
import velox.api.layer1.data.SubscribeInfo;
import velox.api.layer1.data.SubscribeInfoCrypto;
import velox.api.layer1.data.TradeInfo;

/**
 * This example demonstrates how to connect to a single exchange and get its market data (order book and trades).
 */
public class MarketDataSingleExchangeExample {

    private final ConnectivityClient client;

    public MarketDataSingleExchangeExample() {
        client = new ConnectivityClient(Settings.EXCHANGEPORT_TOKEN);
    }

    public void run() {
        // Construct your chosen provider(s) (which is an abstraction of the exchange you want to connect to).
        // You may construct multiple various providers and use the same or different listeners
        // to listen to the data (see below).
        // Binance Futures API enables you to select the frequency of order book updates. At the time of this writing,
        // "100 milliseconds" is the highest available frequency (where order book updates are most frequent).
        Layer1ApiProvider binanceFuturesProvider = client.binanceFutures(false, "100 milliseconds");

        System.out.println("Available symbols in Binance Futures:");
        printAvailableSymbols(binanceFuturesProvider);

        // First, we must specify the pip and the size increment to which the data is going to be rounded.
        // (pip = price increment)
        //
        // The chosen values must be at least the minimum available exchange pip / size instrument
        // for the specified instrument.
        // For example: if BTCUSD_PERP on Binance Futures has a minimum pip of 0.001, you can select 0.01 to
        // make all prices be rounded to 0.01.

        Map<String, Double> selectedPipAtBinanceFutures = new HashMap<String, Double>() {
            {
                put("BTCUSD_PERP", 0.1);
                put("ETHUSD_PERP", 0.01);
            }
        };
        Map<String, Double> selectedSizeIncrementAtBinanceFutures = new HashMap<String, Double>() {
            {
                put("BTCUSD_PERP", 0.001);
                put("ETHUSD_PERP", 0.01);
            }
        };

        // Subscribe to the instrument. After that, order book and trade data will start arriving.
        // Note: See the ConnectionAndInstrumentStatusExample to see how to subscribe to listeners informing you of
        //       whether the subscription has been successful or not.
        binanceFuturesProvider.subscribe(
            new SubscribeInfoCrypto(
                "BTCUSD_PERP",
                null,
                null,
                selectedPipAtBinanceFutures.get("BTCUSD_PERP"),
                1 / selectedSizeIncrementAtBinanceFutures.get("BTCUSD_PERP")
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

        // These methods get called e.g. when a trade happens, or an order book is changed.
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
        new MarketDataSingleExchangeExample().run();
    }
}
