import com.bookmap.connectivitylib.ConnectivityClient;
import java.util.HashMap;
import java.util.Map;
import velox.api.layer1.Layer1ApiAdminAdapter;
import velox.api.layer1.Layer1ApiInstrumentListener;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.data.*;

/**
 * This example demonstrates how to listen to events that inform you regarding the connection status, adding and
 * removing of instruments, etc.
 */
public class ConnectionAndInstrumentStatusExample {

    private final ConnectivityClient client;

    public ConnectionAndInstrumentStatusExample() {
        client = new ConnectivityClient(Settings.EXCHANGEPORT_TOKEN);
    }

    public void run() throws Exception {
        // Construct your chosen provider(s) (which is an abstraction of the exchange you want to connect to).
        // You may construct multiple various providers and use the same or different listeners
        // to listen to the data (see below).
        Layer1ApiProvider krakenFuturesProvider = client.krakenFutures(false);

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

        // Login/connection-related listeners.
        krakenFuturesProvider.addListener(
            new Layer1ApiAdminAdapter() {
                @Override
                public void onLoginFailed(LoginFailedReason reason, String message) {
                    System.out.println("onLoginFailed: " + reason + " " + message);
                }

                @Override
                public void onLoginSuccessful() {
                    System.out.println("onLoginSuccessful");
                }

                @Override
                public void onConnectionLost(DisconnectionReason reason, String message) {
                    System.out.println("onConnectionLost: " + reason + " " + message);
                }

                @Override
                public void onConnectionRestored() {
                    System.out.println("onConnectionRestored");
                }

                @Override
                public void onSystemTextMessage(String message, SystemTextMessageType messageType) {
                    System.out.println("onSystemTextMessage: " + message + " " + messageType);
                }

                @Override
                public void onUserMessage(Object data) {
                    System.out.println("onUserMessage: " + data);
                }
            }
        );

        // Instrument-subscription-related listeners.
        krakenFuturesProvider.addListener(
            new Layer1ApiInstrumentListener() {
                @Override
                public void onInstrumentAdded(String alias, InstrumentInfo instrumentInfo) {
                    System.out.println("onInstrumentAdded: " + alias);
                }

                @Override
                public void onInstrumentRemoved(String alias) {
                    System.out.println("onInstrumentRemoved: " + alias);
                }

                @Override
                public void onInstrumentNotFound(String symbol, String type, String exchange) {
                    System.out.println("onInstrumentNotFound: " + symbol);
                }

                @Override
                public void onInstrumentAlreadySubscribed(String symbol, String type, String exchange) {
                    System.out.println("onInstrumentAlreadySubscribed: " + symbol);
                }
            }
        );

        System.out.println("Subscribing to PI_ETHUSD");
        krakenFuturesProvider.subscribe(
            new SubscribeInfoCrypto(
                "PI_ETHUSD",
                null,
                null,
                selectedPipAtKrakenFutures.get("PI_ETHUSD"),
                1 / selectedSizeIncrementAtKrakenFutures.get("PI_ETHUSD")
            )
        );

        Thread.sleep(3000);

        System.out.println("Unsubscribing from PI_ETHUSD");
        krakenFuturesProvider.unsubscribe("PI_ETHUSD");

        System.out.println("Closing Kraken Futures");
        krakenFuturesProvider.close();
    }

    public static void main(String[] args) throws Exception {
        new ConnectionAndInstrumentStatusExample().run();
    }
}
