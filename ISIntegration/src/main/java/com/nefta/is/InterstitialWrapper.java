package com.nefta.is;

import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;

import com.ironsource.adapters.custom.nefta.NeftaCustomAdapter;
import com.unity3d.mediation.LevelPlayAdError;
import com.unity3d.mediation.LevelPlayAdInfo;
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAd;
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAdListener;

public class InterstitialWrapper implements LevelPlayInterstitialAdListener {
    private MainActivity _activity;
    private Button _loadButton;
    private Button _showButton;
    private LevelPlayInterstitialAd _interstitial;

    public InterstitialWrapper(MainActivity activity, Button loadButton, Button showButton) {
        _activity = activity;
        _loadButton = loadButton;
        _showButton = showButton;

        _loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log("Load");
                _interstitial = new LevelPlayInterstitialAd("wrzl86if1sqfxquc");
                _interstitial.setListener(InterstitialWrapper.this);
                _interstitial.loadAd();
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
    public void onAdLoaded(@NonNull LevelPlayAdInfo adInfo) {
        Log("onAdLoaded " + adInfo);
        _showButton.setEnabled(true);

        NeftaCustomAdapter.OnExternalMediationRequestLoaded(NeftaCustomAdapter.AdType.Interstitial,  0.4, 0.5, adInfo);
    }

    @Override
    public void onAdLoadFailed(@NonNull LevelPlayAdError error) {
        Log("onAdLoadFailed " + error);

        NeftaCustomAdapter.OnExternalMediationRequestFailed(NeftaCustomAdapter.AdType.Interstitial,0.6, 0.7, error);
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
