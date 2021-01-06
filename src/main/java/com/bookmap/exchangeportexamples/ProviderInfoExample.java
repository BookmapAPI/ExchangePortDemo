package com.bookmap.exchangeportexamples;

import com.bookmap.exchangeport.ConnectivityClient;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.data.DefaultAndList;
import velox.api.layer1.data.SubscribeInfo;

public class ProviderInfoExample {
    public static void main(String[] args) {
        ConnectivityClient client = new ConnectivityClient(Settings.EXCHANGEPORT_TOKEN);
        Layer1ApiProvider provider = client.binanceSpot();

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

        System.out.println();
        System.out.println("Supported stop orders: " + provider.getSupportedFeatures().supportedStopOrders);
        System.out.println("Supported limit order durations: " + provider.getSupportedFeatures().supportedLimitDurations);
        System.out.println("Supported stop order durations: " + provider.getSupportedFeatures().supportedStopDurations);
    }
}
