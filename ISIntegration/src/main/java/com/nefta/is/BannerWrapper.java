package com.nefta.is;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import com.ironsource.mediationsdk.ISBannerSize;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.IronSourceBannerLayout;
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo;
import com.ironsource.mediationsdk.impressionData.ImpressionData;
import com.ironsource.mediationsdk.impressionData.ImpressionDataListener;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.LevelPlayBannerListener;

public class BannerWrapper implements LevelPlayBannerListener, ImpressionDataListener {
    private MainActivity _activity;
    private ViewGroup _bannerGroup;
    private Button _loadAndShowButton;
    private Button _closeButton;
    private IronSourceBannerLayout _banner;

    public BannerWrapper(MainActivity activity, ViewGroup bannerGroup, Button loadAndShowButton, Button closeButton) {
        _activity = activity;
        _bannerGroup = bannerGroup;
        _loadAndShowButton = loadAndShowButton;
        _closeButton = closeButton;

        IronSource.addImpressionDataListener(this);

        _loadAndShowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _banner = IronSource.createBanner(_activity, ISBannerSize.BANNER);
                if (_banner != null) {
                    FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
                    bannerGroup.addView(_banner, 0, layoutParams);
                    _banner.setLevelPlayBannerListener(BannerWrapper.this);

                    Log("Load");
                    IronSource.loadBanner(_banner);
                } else {
                    Log("IronSource.createBanner returned null");
                }

                _loadAndShowButton.setEnabled(false);
            }
        });
        _closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log("Close");
                IronSource.destroyBanner(_banner);
                _bannerGroup.removeView(_banner);

                _loadAndShowButton.setEnabled(true);
                _closeButton.setEnabled(false);
            }
        });

        _closeButton.setEnabled(false);
    }

    @Override
    public void onAdLoaded(AdInfo adInfo) {
        Log("onAdLoaded " + adInfo);

        _closeButton.setEnabled(true);
    }

    @Override
    public void onAdLoadFailed(IronSourceError ironSourceError) {
        Log("onAdLoadFailed " + ironSourceError);

        _loadAndShowButton.setEnabled(true);
        _closeButton.setEnabled(false);
    }

    @Override
    public void onAdClicked(AdInfo adInfo) {
        Log("onAdClicked " + adInfo);
    }

    @Override
    public void onAdLeftApplication(AdInfo adInfo) {
        Log("onAdLeftApplication " + adInfo);
    }

    @Override
    public void onAdScreenPresented(AdInfo adInfo) {
        Log("onAdScreenPresented " + adInfo);
    }

    @Override
    public void onAdScreenDismissed(AdInfo adInfo) {
        Log("onAdScreenDismissed " + adInfo);

        _loadAndShowButton.setEnabled(true);
        _closeButton.setEnabled(false);
    }

    @Override
    public void onImpressionSuccess(ImpressionData impressionData) {
        Log("onImpressionSuccess " + impressionData);
    }

    private void Log(String message) {
        _activity.Log("Banner "+ message);
    }
}
