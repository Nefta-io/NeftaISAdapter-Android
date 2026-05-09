package com.nefta.is;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.ironsource.adapters.custom.nefta.NeftaCustomAdapter;
import com.unity3d.mediation.LevelPlayAdError;
import com.unity3d.mediation.LevelPlayAdInfo;
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAd;
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAdListener;

public class InterstitialDefault implements LevelPlayInterstitialAdListener, Interstitial {

    private InterstitialUi _ui;
    private LevelPlayInterstitialAd _interstitial;
    private Handler _handler;

    public void Init(InterstitialUi ui) {
        _ui = ui;
        _handler = new Handler(Looper.getMainLooper());

        _interstitial = new LevelPlayInterstitialAd(Interstitial.AdUnitA);
        _interstitial.setListener(this);
    }

    @Override
    public void onAdLoadFailed(@NonNull LevelPlayAdError error) {
        NeftaCustomAdapter.OnExternalMediationRequestFailed(error);

        Log("Load failed : "+ error.getAdUnitId() + ": "+ error.getErrorMessage());

        _handler.postDelayed(() -> {
            if (_ui.IsAutoLoad) {
                Load();
            }
        }, 5000);
    }

    @Override
    public void onAdLoaded(@NonNull LevelPlayAdInfo adInfo) {
        NeftaCustomAdapter.OnExternalMediationRequestLoaded(adInfo);

        Log("Loaded " + adInfo.getAdUnitId() + " at: "+ adInfo.getRevenue());

        _ui.SetAvailability(true);
    }

    @Override
    public void onAdClicked(@NonNull LevelPlayAdInfo adInfo) {
        NeftaCustomAdapter.OnExternalMediationClick(adInfo);

        Log("onAdClicked " + adInfo);
    }

    @Override
    public void onAdDisplayFailed(@NonNull LevelPlayAdError error, @NonNull LevelPlayAdInfo info) {
        Log("onAdDisplayFailed = " + info + ": " + error);

        if (_ui.IsAutoLoad) {
            Load();
        }
    }

    @Override
    public void onAdDisplayed(@NonNull LevelPlayAdInfo adInfo) {
        Log("onAdDisplayed " + adInfo);
    }

    @Override
    public void onAdClosed(@NonNull LevelPlayAdInfo adInfo) {
        Log("onAdClosed " + adInfo);

        if (_ui.IsAutoLoad) {
            Load();
        }
    }

    public void Load() {
        NeftaCustomAdapter.OnExternalMediationRequest(_interstitial);
        _interstitial.loadAd();
    }

    public void Show() {
        if (_interstitial.isAdReady()) {
            _interstitial.showAd(_ui.Activity);
        } else if (_ui.IsAutoLoad) {
            Load();
        }

        _ui.SetAvailability(false);
    }

    private void Log(String log) {
        _ui.Log(log);
    }
}
