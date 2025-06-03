package com.nefta.is;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;

import com.ironsource.adapters.custom.nefta.NeftaCustomAdapter;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.WaterfallConfiguration;
import com.nefta.sdk.Insight;
import com.nefta.sdk.NeftaPlugin;
import com.unity3d.mediation.LevelPlayAdError;
import com.unity3d.mediation.LevelPlayAdInfo;
import com.unity3d.mediation.LevelPlayAdSize;
import com.unity3d.mediation.banner.LevelPlayBannerAdView;
import com.unity3d.mediation.banner.LevelPlayBannerAdViewListener;

import java.util.HashMap;

public class BannerWrapper implements LevelPlayBannerAdViewListener {

    private final String FloorPriceInsightName = "calculated_user_floor_price_banner";

    private double _requestedBidFloor;
    private double _calculatedBidFloor;
    private boolean _isLoadRequested;

    private MainActivity _activity;
    private ViewGroup _bannerGroup;
    private Button _loadAndShowButton;
    private Button _closeButton;
    private Handler _handler;

    private LevelPlayBannerAdView _banner;

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

        Log("OnBehaviourInsights for Banner calculated bid floor: "+ _calculatedBidFloor);

        if (_isLoadRequested) {
            Load();
        }
    }

    private void Load() {
        _isLoadRequested = false;

        if (_calculatedBidFloor <= 0) {
            _requestedBidFloor = 0;
            IronSource.setWaterfallConfiguration(WaterfallConfiguration.empty(), IronSource.AD_UNIT.BANNER);
        } else {
            _requestedBidFloor = _calculatedBidFloor;
            WaterfallConfiguration.WaterfallConfigurationBuilder builder = WaterfallConfiguration.builder();
            WaterfallConfiguration waterfallConfiguration = builder
                    .setFloor(_requestedBidFloor)
                    .build();
            IronSource.setWaterfallConfiguration(waterfallConfiguration, IronSource.AD_UNIT.BANNER);
        }

        Log("Loading Banner with floor: "+ _requestedBidFloor);

        _banner = new LevelPlayBannerAdView(_activity, "vpkt794d6ruyfwr4");
        _banner.setAdSize(LevelPlayAdSize.createAdaptiveAdSize(_activity));
        _bannerGroup.addView(_banner);
        _banner.setBannerListener(BannerWrapper.this);

        _banner.loadAd();
    }

    @Override
    public void onAdLoadFailed(@NonNull LevelPlayAdError error) {
        NeftaCustomAdapter.OnExternalMediationRequestFailed(NeftaCustomAdapter.AdType.Banner, _requestedBidFloor, _calculatedBidFloor, error);

        Log("onAdLoadFailed " + error);

        _loadAndShowButton.setEnabled(true);
        _closeButton.setEnabled(false);
    }

    @Override
    public void onAdLoaded(@NonNull LevelPlayAdInfo adInfo) {
        NeftaCustomAdapter.OnExternalMediationRequestLoaded(NeftaCustomAdapter.AdType.Banner, _requestedBidFloor, _calculatedBidFloor, adInfo);

        Log("onAdLoaded " + adInfo);
    }

    public BannerWrapper(MainActivity activity, ViewGroup bannerGroup, Button loadAndShowButton, Button closeButton) {
        _activity = activity;
        _bannerGroup = bannerGroup;
        _loadAndShowButton = loadAndShowButton;
        _closeButton = closeButton;

        _handler = new Handler(Looper.getMainLooper());

        _loadAndShowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GetInsightsAndLoad();

                Log("Loading ...");

                _loadAndShowButton.setEnabled(false);
                _closeButton.setEnabled(true);
            }
        });
        _closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (_banner != null) {
                    _banner.destroy();
                    _bannerGroup.removeView(_banner);
                    _banner = null;
                }

                Log("Closing banner");

                _loadAndShowButton.setEnabled(true);
                _closeButton.setEnabled(false);
            }
        });

        _loadAndShowButton.setEnabled(false);
        _closeButton.setEnabled(false);
    }

    public void OnReady() {
        _loadAndShowButton.setEnabled(true);
    }

    @Override
    public void onAdDisplayFailed(@NonNull LevelPlayAdInfo adInfo, @NonNull LevelPlayAdError error) {
        Log("onAdDisplayFailed " + adInfo + " error: "+ error);
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
    public void onAdLeftApplication(@NonNull LevelPlayAdInfo adInfo) {
        Log("onAdLeftApplication " + adInfo);
    }

    @Override
    public void onAdExpanded(@NonNull LevelPlayAdInfo adInfo) {
        Log("onAdExpanded " + adInfo);
    }

    @Override
    public void onAdCollapsed(@NonNull LevelPlayAdInfo adInfo) {
        Log("onAdCollapsed " + adInfo);
    }

    private void Log(String message) {
        _activity.Log("Banner "+ message);
    }
}
