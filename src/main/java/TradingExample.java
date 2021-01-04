import com.bookmap.exchangeport.ConnectivityClient;
import velox.api.layer1.Layer1ApiAdminListener;
import velox.api.layer1.Layer1ApiDataListener;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.Layer1ApiTradingListener;
import velox.api.layer1.data.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This example demonstrates how to connect to Deribit, subscribe to market data, perform some order manipulation.
 *
 * An buy BTC-PERPETUAL order is placed at price $25,000, then the order's price is updated five times (each time
 * reduced by $1,000), then the order is cancelled.
 */
public class TradingExample {

    private static final Map<String, Double> SELECTED_PIP_AT_DERIBIT = new HashMap<String, Double>() {
        {
            put("BTC-PERPETUAL", 0.5);
        }
    };
    private static final Map<String, Double> SELECTED_SIZE_INCREMENT_AT_DERIBIT = new HashMap<String, Double>() {
        {
            put("BTC-PERPETUAL", 10.);
        }
    };

    private final ConnectivityClient client;

    /**
     * Tracks our open orders (quotes). Maps the order ID to the order object.
     */
    private final Map<String, OrderInfoUpdate> workingOrders = new ConcurrentHashMap<>();

    public TradingExample() {
        client = new ConnectivityClient(Settings.EXCHANGEPORT_TOKEN);
    }

    public void run() throws Exception {
        Layer1ApiProvider deribitProvider = client.deribit(
            true,
            Settings.DERIBIT_TESTNET_API_KEY,
            Settings.DERIBIT_TESTNET_API_SECRET
        );


        // These methods get called e.g. when a trade happens, or an order book is changed.
        deribitProvider.addListener(
            new Layer1ApiDataListener() {
                @Override
                public void onTrade(String alias, double priceLevel, int sizeLevel, TradeInfo tradeInfo) {
                    String buyOrSell = tradeInfo.isBidAggressor ? "BUY" : "SELL";
                    double price = priceLevel * SELECTED_PIP_AT_DERIBIT.get(alias);
                    double size = sizeLevel * SELECTED_SIZE_INCREMENT_AT_DERIBIT.get(alias);
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
                    double price = priceLevel * SELECTED_PIP_AT_DERIBIT.get(alias);
                    double size = sizeLevel * SELECTED_SIZE_INCREMENT_AT_DERIBIT.get(alias);
//                    System.out.printf("onDepth: alias=%s side=%s price=%s size=%s %n", alias, side, price, size);
                }

                @Override
                public void onMarketMode(String s, MarketMode marketMode) {
                    // Not used in blockchain exchanges.
                }
            }
        );

        // Let's a listener so we get the updates what's happening with our orders.
        deribitProvider.addListener(
            new Layer1ApiTradingListener() {
                @Override
                public void onOrderUpdated(OrderInfoUpdate orderInfoUpdate) {
                    // Called when an order has been placed/modified/canceled.
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
                    // Called when the account status - frequency depends on the exchange.
                    System.out.println(statusInfo);
                }

                @Override
                public void onBalance(BalanceInfo balanceInfo) {
                    // Called when the balance changes - frequency depends on the exchange.
                    System.out.println(balanceInfo);
                }
            }
        );

        deribitProvider.addListener(
            new Layer1ApiAdminListener() {
                @Override
                public void onLoginFailed(LoginFailedReason loginFailedReason, String s) {}

                @Override
                public void onLoginSuccessful() {}

                @Override
                public void onConnectionLost(DisconnectionReason disconnectionReason, String s) {}

                @Override
                public void onConnectionRestored() {}

                @Override
                public void onSystemTextMessage(String s, SystemTextMessageType systemTextMessageType) {
                    System.out.println("System message: " + s);
                }

                @Override
                public void onUserMessage(Object o) {}
            }
        );

        // Subscribe to the instrument. After that, order book and trade data will start arriving.
        // Note: See the ConnectionAndInstrumentStatusExample to see how to subscribe to listeners informing you of
        //       whether the subscription has been successful or not.
        deribitProvider.subscribe(
            new SubscribeInfoCrypto(
                "BTC-PERPETUAL",
                null,
                null,
                SELECTED_PIP_AT_DERIBIT.get("BTC-PERPETUAL"),
                1 / SELECTED_SIZE_INCREMENT_AT_DERIBIT.get("BTC-PERPETUAL")
            )
        );

        // Instead of waiting for an arbitrary amount of time and assuming the subscription is successful, you should
        // wait for a callback that confirms that (or reports an error) - see ConnectionAndInstrumentStatusExample.
        // But for the sake of brevity this is avoided here.
        Thread.sleep(3000);

        // Let's store the existing order IDs that are open in this account, so we can avoid them in the tests below.
        Set<String> existingOrderIds = new HashSet<>(workingOrders.keySet());

        double limitPrice = 25000.0;
        // Deribit measures sizes in USD. This value should be a multiplier of the selected size increment.
        double size = 20;
        System.out.println("Sending order");
        deribitProvider.sendOrder(
            new SimpleOrderSendParameters(
                "BTC-PERPETUAL",
                true,
                (int) Math.round(size / SELECTED_SIZE_INCREMENT_AT_DERIBIT.get("BTC-PERPETUAL")),
                OrderDuration.GTC,
                "MY_ORDER_1",
                limitPrice,
                Double.NaN,
                0,
                0,
                0,
                0,
                false
            )
        );
        System.out.println("Order sent");

        String orderId = waitForOrder(existingOrderIds);
        System.out.println("Received order confirmation, the order ID is: " + orderId);

        // Let's update the order's price a few times.
        for (int i = 0; i < 5; i++) {
            limitPrice -= 1000;
            deribitProvider.updateOrder(new OrderMoveParameters(orderId, Double.NaN, limitPrice));
            waitForOrderToBeUpdated(orderId, limitPrice);

            // Wait a bit, so we can observe (e.g. in the Deribit web platform) what is going on.
            Thread.sleep(2000);
        }

        System.out.println("Canceling order ID " + orderId);
        deribitProvider.updateOrder(new OrderCancelParameters(orderId));
    }

    private String waitForOrder(Set<String> existingOrderIds) {
        while (true) {
            System.out.println("Waiting for the order response.");

            for (String orderId : workingOrders.keySet()) {
                // An order ID that hasn't existed before is the ID of our new order.
                if (!existingOrderIds.contains(orderId)) {
                    return orderId;
                }
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void waitForOrderToBeUpdated(String orderId, double expectedPrice) {
        while (true) {
            OrderInfoUpdate order = workingOrders.get(orderId);
            if (order == null)
                throw new RuntimeException("The order ID " + orderId + " is open any more.");

            // Floating point numbers cannot be reliably compared, so we convert the prices to "price levels" - which
            // are integer multipliers of the selected pip.
            double pip = SELECTED_PIP_AT_DERIBIT.get("BTC-PERPETUAL");
            long currentPriceLevel = Math.round(order.limitPrice * pip);
            long expectedPriceLevel = Math.round(expectedPrice * pip);
            if (currentPriceLevel == expectedPriceLevel) {
                System.out.println("Order ID " + orderId + " has been moved to price " + expectedPrice);
                break;
            }

            System.out.printf(
                "Waiting for order ID %s to be moved to price %s. Currently, it is at %s.%n",
                orderId,
                expectedPrice, order.limitPrice
            );

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new TradingExample().run();
    }
}
