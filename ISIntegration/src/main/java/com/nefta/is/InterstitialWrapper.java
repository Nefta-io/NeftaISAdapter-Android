package com.nefta.is;

import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;

import com.ironsource.adapters.custom.nefta.NeftaCustomAdapter;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo;
import com.ironsource.mediationsdk.impressionData.ImpressionData;
import com.ironsource.mediationsdk.impressionData.ImpressionDataListener;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.LevelPlayInterstitialListener;
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

        _showButton.setEnabled(false);
    }

    @Override
    public void onAdLoaded(@NonNull LevelPlayAdInfo info) {
        Log("onAdLoaded " + info);
        _showButton.setEnabled(true);

        NeftaCustomAdapter.OnExternalAdLoad(NeftaCustomAdapter.AdType.Interstitial,  0.4);
    }

    @Override
    public void onAdLoadFailed(@NonNull LevelPlayAdError error) {
        Log("onAdLoadFailed " + error);

        NeftaCustomAdapter.OnExternalAdFail(NeftaCustomAdapter.AdType.Interstitial,0.6, error);
    }

    @Override
    public void onAdDisplayFailed(@NonNull LevelPlayAdError error, @NonNull LevelPlayAdInfo info) {
        Log("onAdDisplayFailed = " + info + ": " + error);
    }

    @Override
    public void onAdDisplayed(@NonNull LevelPlayAdInfo levelPlayAdInfo) {
        Log("onAdDisplayed " + levelPlayAdInfo);

    }

    @Override
    public void onAdClicked(@NonNull LevelPlayAdInfo levelPlayAdInfo) {
        Log("onAdClicked " + levelPlayAdInfo);
    }

    @Override
    public void onAdClosed(@NonNull LevelPlayAdInfo levelPlayAdInfo) {
        Log("onAdClosed " + levelPlayAdInfo);
    }

    @Override
    public void onAdInfoChanged(@NonNull LevelPlayAdInfo levelPlayAdInfo) {
        Log("onAdInfoChanged " + levelPlayAdInfo);
    }


    void Log(String log) {
        _activity.Log("Interstitial " + log);
    }
}
