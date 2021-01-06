package com.bookmap.exchangeportexamples;

import com.bookmap.exchangeport.ConnectivityClient;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.data.DefaultAndList;
import velox.api.layer1.data.SubscribeInfoCrypto;

public class GetInstrumentPipAndSizeIncrementExample {

    public static void main(String[] args) {
        ConnectivityClient client = new ConnectivityClient(Settings.EXCHANGEPORT_TOKEN);

        Layer1ApiProvider provider = client.krakenFutures(false);

        SubscribeInfoCrypto subscribeInfo = new SubscribeInfoCrypto("PI_ETHUSD", null, null, 0.1, 0.01);

        DefaultAndList<Double> pips = provider.getSupportedFeatures().pipsFunction.apply(subscribeInfo);
        double minPip = pips.valueOptions.stream().min(Double::compareTo).get();
        System.out.println("The instrument pip (minimum available pip) for PI_ETHUSD is: " + minPip);

        DefaultAndList<Double> sizes = provider.getSupportedFeatures().sizeMultiplierFunction.apply(subscribeInfo);
        double minSizeIncrement = 1 / sizes.valueOptions.stream().max(Double::compareTo).get();
        System.out.println(
            "The instrument size increment (minimum available size increment) for PI_ETHUSD is: " + minSizeIncrement
        );
    }
}
