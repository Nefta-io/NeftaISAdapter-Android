package com.nefta.is;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;

import com.ironsource.adapters.custom.nefta.NeftaCustomAdapter;
import com.nefta.sdk.AdInsight;
import com.nefta.sdk.Insights;
import com.nefta.sdk.NeftaPlugin;
import com.unity3d.mediation.LevelPlayAdError;
import com.unity3d.mediation.LevelPlayAdInfo;
import com.unity3d.mediation.LevelPlayAdSize;
import com.unity3d.mediation.banner.LevelPlayBannerAdView;
import com.unity3d.mediation.banner.LevelPlayBannerAdViewListener;

public class BannerWrapper implements LevelPlayBannerAdViewListener {

    private MainActivity _activity;
    private ViewGroup _bannerGroup;
    private Button _loadAndShowButton;
    private Button _closeButton;
    private Handler _handler;

    private LevelPlayBannerAdView _banner;
    private AdInsight _usedInsight;
    private double _requestedFloorPrice;

    private void GetInsightsAndLoad() {
        NeftaPlugin._instance.GetInsights(Insights.BANNER, this::Load, 5);
    }

    private void Load(Insights insights) {
        _requestedFloorPrice = 0;
        _usedInsight = insights._banner;
        if (_usedInsight != null) {
            _requestedFloorPrice = _usedInsight._floorPrice;
        }

        Log("Loading Banner with floor: "+ _requestedFloorPrice);

        LevelPlayBannerAdView.Config config = new LevelPlayBannerAdView.Config.Builder()
                .setAdSize(LevelPlayAdSize.BANNER)
                .setBidFloor(_requestedFloorPrice).build();

        _banner = new LevelPlayBannerAdView(_activity, "vpkt794d6ruyfwr4", config);
        _bannerGroup.addView(_banner);
        _banner.setBannerListener(BannerWrapper.this);

        _banner.loadAd();
    }

    @Override
    public void onAdLoadFailed(@NonNull LevelPlayAdError error) {
        NeftaCustomAdapter.OnExternalMediationRequestFailed(NeftaCustomAdapter.AdType.Banner, _usedInsight, _requestedFloorPrice, error);

        Log("onAdLoadFailed " + error);

        _loadAndShowButton.setEnabled(true);
        _closeButton.setEnabled(false);
    }

    @Override
    public void onAdLoaded(@NonNull LevelPlayAdInfo adInfo) {
        NeftaCustomAdapter.OnExternalMediationRequestLoaded(NeftaCustomAdapter.AdType.Banner, _usedInsight, _requestedFloorPrice, adInfo);

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
