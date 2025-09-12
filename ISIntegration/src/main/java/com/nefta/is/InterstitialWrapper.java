package com.nefta.is;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

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

    private static final String _dynamicAdUnitId = "0u6jgm23ggqso85n";
    private static final String _defaultAdUnitId = "wrzl86if1sqfxquc";

    private LevelPlayInterstitialAd _dynamicInterstitial;
    private double _dynamicAdRevenue = -1;
    private AdInsight _dynamicInsight;
    private LevelPlayInterstitialAd _defaultInterstitial;
    private double _defaultAdRevenue = -1;

    private MainActivity _activity;
    private final Switch _loadSwitch;
    private Button _showButton;
    private Handler _handler;

    private void StartLoading() {
        if (_dynamicInterstitial == null) {
            GetInsightsAndLoad(null);
        }
        if (_defaultInterstitial == null) {
            LoadDefault();
        }
    }

    private void GetInsightsAndLoad(AdInsight previousinsight) {
        NeftaPlugin._instance.GetInsights(Insights.INTERSTITIAL, previousinsight, this::LoadWithInsights, 5);
    }

    private void LoadWithInsights(Insights insights) {
        _dynamicInsight = insights._interstitial;
        if (_dynamicInsight != null) {
            Log("Loading Dynamic with floor: " + _dynamicInsight._floorPrice);

            LevelPlayInterstitialAd.Config config = new LevelPlayInterstitialAd.Config.Builder()
                    .setBidFloor(_dynamicInsight._floorPrice).build();
            _dynamicInterstitial = new LevelPlayInterstitialAd(_dynamicAdUnitId, config);
            _dynamicInterstitial.setListener(InterstitialWrapper.this);
            _dynamicInterstitial.loadAd();

            NeftaCustomAdapter.OnExternalMediationRequest(_dynamicInterstitial, _dynamicInsight);
        }
    }

    private void LoadDefault() {
        Log("Loading Default");

        _defaultInterstitial = new LevelPlayInterstitialAd(_defaultAdUnitId);
        _defaultInterstitial.setListener(this);
        _defaultInterstitial.loadAd();

        NeftaCustomAdapter.OnExternalMediationRequest(_defaultInterstitial);
    }

    @Override
    public void onAdLoadFailed(@NonNull LevelPlayAdError error) {
        NeftaCustomAdapter.OnExternalMediationRequestFailed(error);

        if (_dynamicInterstitial != null && _dynamicInterstitial.getAdId().equals(error.getAdId())) {
            Log("onAdLoadFailed Dynamic: "+ error);

            _dynamicInterstitial = null;
            _handler.postDelayed(() -> {
                if (_loadSwitch.isChecked()) {
                    GetInsightsAndLoad(_dynamicInsight);
                }
            }, 5000);
        } else {
            Log("onAdLoadFailed Default: "+ error);

            _defaultInterstitial = null;
            _handler.postDelayed(() -> {
                if (_loadSwitch.isChecked()) {
                    LoadDefault();
                }
            }, 5000);
        }
    }

    @Override
    public void onAdLoaded(@NonNull LevelPlayAdInfo adInfo) {
        NeftaCustomAdapter.OnExternalMediationRequestLoaded(adInfo);
        if (_dynamicInterstitial != null && _dynamicInterstitial.getAdId().equals(adInfo.getAdId())) {
            Log("onAdLoaded Dynamic " + adInfo);

            _dynamicAdRevenue = adInfo.getRevenue();
        } else {
            Log("onAdLoaded Default " + adInfo);

            _defaultAdRevenue = adInfo.getRevenue();
        }

        UpdateShowButton();
        _showButton.setEnabled(true);
    }

    @Override
    public void onAdClicked(@NonNull LevelPlayAdInfo adInfo) {
        NeftaCustomAdapter.OnExternalMediationClick(adInfo);

        Log("onAdClicked " + adInfo);
    }

    public InterstitialWrapper(MainActivity activity, Switch loadButton, Button showButton) {
        _activity = activity;
        _loadSwitch = loadButton;
        _showButton = showButton;

        _handler = new Handler(Looper.getMainLooper());

        _loadSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    StartLoading();
                }
            }
        });
        _showButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isShown = false;
                if (_dynamicAdRevenue >= 0) {
                    if (_defaultAdRevenue > _dynamicAdRevenue) {
                        isShown = TryShowDefault();
                    }
                    if (!isShown) {
                        isShown = TryShowDynamic();
                    }
                }
                if (!isShown && _defaultAdRevenue >= 0) {
                    TryShowDefault();
                }
                UpdateShowButton();
            }
        });

        _loadSwitch.setEnabled(false);
        _showButton.setEnabled(false);
    }

    private boolean TryShowDynamic() {
        boolean isShown = false;
        if (_dynamicInterstitial.isAdReady()) {
            _dynamicInterstitial.showAd(_activity);
            isShown = true;
        }
        _dynamicAdRevenue = -1;
        _dynamicInterstitial = null;
        return isShown;
    }

    private boolean TryShowDefault() {
        boolean isShown = false;
        if (_defaultInterstitial.isAdReady()) {
            _defaultInterstitial.showAd(_activity);
            isShown = true;
        }
        _defaultAdRevenue = -1;
        _defaultInterstitial = null;
        return isShown;
    }

    public void OnReady() {
        _loadSwitch.setEnabled(true);
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
    public void onAdClosed(@NonNull LevelPlayAdInfo adInfo) {
        Log("onAdClosed " + adInfo);

        // start new load cycle
        if (_loadSwitch.isChecked()) {
            StartLoading();
        }
    }

    @Override
    public void onAdInfoChanged(@NonNull LevelPlayAdInfo adInfo) {
        Log("onAdInfoChanged " + adInfo);
    }

    private void Log(String log) {
        _activity.Log("Interstitial " + log);
    }

    private void UpdateShowButton() {
        _showButton.setEnabled(_dynamicAdRevenue >= 0 || _defaultAdRevenue >= 0);
    }
}
