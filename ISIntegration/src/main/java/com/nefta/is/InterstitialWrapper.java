package com.nefta.is;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;

import com.ironsource.adapters.custom.nefta.NeftaCustomAdapter;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.WaterfallConfiguration;
import com.nefta.sdk.Insight;
import com.nefta.sdk.NeftaPlugin;
import com.unity3d.mediation.LevelPlayAdError;
import com.unity3d.mediation.LevelPlayAdInfo;
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAd;
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAdListener;

import java.util.HashMap;

public class InterstitialWrapper implements LevelPlayInterstitialAdListener {

    private final String FloorPriceInsightName = "calculated_user_floor_price_interstitial";

    private double _requestedBidFloor;
    private double _calculatedBidFloor;
    private boolean _isLoadRequested;

    private MainActivity _activity;
    private Button _loadButton;
    private Button _showButton;
    private Handler _handler;

    private LevelPlayInterstitialAd _interstitial;

    private void GetInsightsAndLoad() {
        _isLoadRequested = true;

        NeftaPlugin._instance.GetBehaviourInsight(new String[] { FloorPriceInsightName }, this::OnBehaviourInsight);

        _handler.postDelayed(() -> {
            if (_isLoadRequested) {
                _calculatedBidFloor = 0;
                Load();
            }
        }, 5000);
    }

    private void OnBehaviourInsight(HashMap<String, Insight> insights) {
        _calculatedBidFloor = 0;
        if (insights.containsKey(FloorPriceInsightName)) {
            _calculatedBidFloor = insights.get(FloorPriceInsightName)._float;
        }

        Log("OnBehaviourInsights for Interstitial calculated bid floor: "+ _calculatedBidFloor);

        if (_isLoadRequested) {
            Load();
        }
    }

    private void Load() {
        _isLoadRequested = false;

        if (_calculatedBidFloor <= 0) {
            _requestedBidFloor = 0;
            IronSource.setWaterfallConfiguration(WaterfallConfiguration.empty(), IronSource.AD_UNIT.INTERSTITIAL);
        } else {
            _requestedBidFloor = _calculatedBidFloor;
            WaterfallConfiguration.WaterfallConfigurationBuilder builder = WaterfallConfiguration.builder();
            WaterfallConfiguration waterfallConfiguration = builder
                    .setFloor(_requestedBidFloor)
                    .build();
            IronSource.setWaterfallConfiguration(waterfallConfiguration, IronSource.AD_UNIT.INTERSTITIAL);
        }

        Log("Loading Interstitial with floor: "+ _requestedBidFloor);

        _interstitial = new LevelPlayInterstitialAd("wrzl86if1sqfxquc");
        _interstitial.setListener(InterstitialWrapper.this);
        _interstitial.loadAd();
    }

    @Override
    public void onAdLoadFailed(@NonNull LevelPlayAdError error) {
        NeftaCustomAdapter.OnExternalMediationRequestFailed(NeftaCustomAdapter.AdType.Interstitial, _requestedBidFloor, _calculatedBidFloor, error);

        Log("onAdLoadFailed " + error);

        _loadButton.setEnabled(true);
        _showButton.setEnabled(false);
        
        _handler.postDelayed(this::GetInsightsAndLoad, 5000);
    }

    @Override
    public void onAdLoaded(@NonNull LevelPlayAdInfo adInfo) {
        NeftaCustomAdapter.OnExternalMediationRequestLoaded(NeftaCustomAdapter.AdType.Interstitial, _requestedBidFloor, _calculatedBidFloor, adInfo);

        Log("onAdLoaded " + adInfo);

        _showButton.setEnabled(true);
    }

    public InterstitialWrapper(MainActivity activity, Button loadButton, Button showButton) {
        _activity = activity;
        _loadButton = loadButton;
        _showButton = showButton;

        _handler = new Handler(Looper.getMainLooper());

        _loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GetInsightsAndLoad();
            }
        });
        _showButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (_interstitial.isAdReady()) {
                    Log("Show");
                    _interstitial.showAd(_activity);
                } else {
                    Log("Not ready");
                }

                _showButton.setEnabled(false);
            }
        });

        _loadButton.setEnabled(false);
        _showButton.setEnabled(false);
    }

    public void OnReady() {
        _loadButton.setEnabled(true);
    }

    @Override
    public void onAdDisplayFailed(@NonNull LevelPlayAdError error, @NonNull LevelPlayAdInfo info) {
        Log("onAdDisplayFailed = " + info + ": " + error);
    }

    @Override
    public void onAdDisplayed(@NonNull LevelPlayAdInfo adInfo) {
        Log("onAdDisplayed " + adInfo);
    }

    @Override
    public void onAdClicked(@NonNull LevelPlayAdInfo adInfo) {
        Log("onAdClicked " + adInfo);
    }

    @Override
    public void onAdClosed(@NonNull LevelPlayAdInfo adInfo) {
        Log("onAdClosed " + adInfo);
    }

    @Override
    public void onAdInfoChanged(@NonNull LevelPlayAdInfo adInfo) {
        Log("onAdInfoChanged " + adInfo);
    }


    void Log(String log) {
        _activity.Log("Interstitial " + log);
    }
}
