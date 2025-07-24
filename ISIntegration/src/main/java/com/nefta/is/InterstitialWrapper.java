package com.nefta.is;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;

import com.ironsource.adapters.custom.nefta.NeftaCustomAdapter;
import com.nefta.sdk.AdInsight;
import com.nefta.sdk.Insights;
import com.nefta.sdk.NeftaPlugin;
import com.unity3d.mediation.LevelPlayAdError;
import com.unity3d.mediation.LevelPlayAdInfo;
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAd;
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAdListener;

public class InterstitialWrapper implements LevelPlayInterstitialAdListener {

    private MainActivity _activity;
    private Button _loadButton;
    private Button _showButton;
    private Handler _handler;
    private boolean _isLoading;

    private LevelPlayInterstitialAd _interstitial;
    private AdInsight _usedInsight;
    private double _requestedFloorPrice;

    private void GetInsightsAndLoad() {
        NeftaPlugin._instance.GetInsights(Insights.INTERSTITIAL, this::Load, 5);
    }

    private void Load(Insights insights) {
        _requestedFloorPrice = 0;
        _usedInsight = insights._interstitial;
        if (_usedInsight != null) {
            _requestedFloorPrice = _usedInsight._floorPrice;
        }

        Log("Loading Interstitial with floor: "+ _requestedFloorPrice);

        LevelPlayInterstitialAd.Config config = new LevelPlayInterstitialAd.Config.Builder()
                .setBidFloor(_requestedFloorPrice).build();

        _interstitial = new LevelPlayInterstitialAd("wrzl86if1sqfxquc", config);
        _interstitial.setListener(InterstitialWrapper.this);
        _interstitial.loadAd();
    }

    @Override
    public void onAdLoadFailed(@NonNull LevelPlayAdError error) {
        NeftaCustomAdapter.OnExternalMediationRequestFailed(NeftaCustomAdapter.AdType.Interstitial, _usedInsight, _requestedFloorPrice, error);

        Log("onAdLoadFailed " + error);
        
        _handler.postDelayed(() -> {
            if (_isLoading) {
                GetInsightsAndLoad();
            }
        }, 5000);
    }

    @Override
    public void onAdLoaded(@NonNull LevelPlayAdInfo adInfo) {
        NeftaCustomAdapter.OnExternalMediationRequestLoaded(NeftaCustomAdapter.AdType.Interstitial, _usedInsight, _requestedFloorPrice, adInfo);

        Log("onAdLoaded " + adInfo);

        SetLoadingButton(false);
        _loadButton.setEnabled(false);
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
                if (_isLoading) {
                    SetLoadingButton(false);
                } else {
                    Log("GetInsightsAndLoad...");
                    GetInsightsAndLoad();
                    SetLoadingButton(true);
                }
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

                _loadButton.setEnabled(true);
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

    private void Log(String log) {
        _activity.Log("Interstitial " + log);
    }

    private void SetLoadingButton(boolean isLoading) {
        _isLoading = isLoading;
        if (isLoading) {
            _loadButton.setText("Cancel");
        } else {
            _loadButton.setText("Load Interstitial");
        }
    }
}
