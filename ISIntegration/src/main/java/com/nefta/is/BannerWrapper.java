package com.nefta.is;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;

import com.ironsource.adapters.custom.nefta.NeftaCustomAdapter;
import com.unity3d.mediation.LevelPlayAdError;
import com.unity3d.mediation.LevelPlayAdInfo;
import com.unity3d.mediation.banner.LevelPlayBannerAdView;
import com.unity3d.mediation.banner.LevelPlayBannerAdViewListener;

public class BannerWrapper implements LevelPlayBannerAdViewListener {
    private MainActivity _activity;
    private ViewGroup _bannerGroup;
    private Button _loadAndShowButton;
    private Button _closeButton;
    private LevelPlayBannerAdView _banner;

    public BannerWrapper(MainActivity activity, ViewGroup bannerGroup, Button loadAndShowButton, Button closeButton) {
        _activity = activity;
        _bannerGroup = bannerGroup;
        _loadAndShowButton = loadAndShowButton;
        _closeButton = closeButton;

        _loadAndShowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log("Load");
                _banner = new LevelPlayBannerAdView(activity, "vpkt794d6ruyfwr4");
                bannerGroup.addView(_banner);
                _banner.setBannerListener(BannerWrapper.this);


                _banner.loadAd();

                _loadAndShowButton.setEnabled(false);
            }
        });
        _closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log("Close");
                _banner.destroy();
                _bannerGroup.removeView(_banner);
                _banner = null;

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
    public void onAdLoaded(@NonNull LevelPlayAdInfo adInfo) {
        Log("onAdLoaded " + adInfo);

        _closeButton.setEnabled(true);

        NeftaCustomAdapter.OnExternalMediationRequestLoaded(NeftaCustomAdapter.AdType.Banner, 0.3, 0.4, adInfo);
    }

    @Override
    public void onAdLoadFailed(@NonNull LevelPlayAdError error) {
        Log("onAdLoadFailed " + error);

        _loadAndShowButton.setEnabled(true);
        _closeButton.setEnabled(false);

        NeftaCustomAdapter.OnExternalMediationRequestFailed(NeftaCustomAdapter.AdType.Banner, 0.6, 0.7, error);
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
        Log("onAdScreenPresented " + adInfo);
    }

    @Override
    public void onAdCollapsed(@NonNull LevelPlayAdInfo adInfo) {
        Log("onAdScreenDismissed " + adInfo);

        _loadAndShowButton.setEnabled(true);
        _closeButton.setEnabled(false);
    }

    private void Log(String message) {
        _activity.Log("Banner "+ message);
    }
}
