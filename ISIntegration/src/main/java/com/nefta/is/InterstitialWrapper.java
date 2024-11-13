package com.nefta.is;

import android.view.View;
import android.widget.Button;

import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo;
import com.ironsource.mediationsdk.impressionData.ImpressionData;
import com.ironsource.mediationsdk.impressionData.ImpressionDataListener;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.LevelPlayInterstitialListener;

public class InterstitialWrapper implements LevelPlayInterstitialListener, ImpressionDataListener {
    private MainActivity _activity;
    private Button _loadButton;
    private Button _showButton;

    public InterstitialWrapper(MainActivity activity, Button loadButton, Button showButton) {
        _activity = activity;
        _loadButton = loadButton;
        _showButton = showButton;

        IronSource.addImpressionDataListener(this);
        IronSource.setLevelPlayInterstitialListener(InterstitialWrapper.this);

        _loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log("Load");
                IronSource.loadInterstitial();
            }
        });
        _showButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (IronSource.isInterstitialReady()) {
                    Log("Show");
                    IronSource.showInterstitial();
                } else {
                    Log("Not ready");
                }

                _showButton.setEnabled(false);
            }
        });

        _showButton.setEnabled(false);
    }

    @Override
    public void onAdReady(AdInfo adInfo) {
        Log("onAdReady " + adInfo);
        _showButton.setEnabled(true);
    }

    @Override
    public void onAdLoadFailed(IronSourceError error) {
        Log("onAdLoadFailed " + error);
    }

    @Override
    public void onAdOpened(AdInfo adInfo) {
        Log("onAdOpened " + adInfo);

    }

    @Override
    public void onAdShowSucceeded(AdInfo adInfo) {
        Log("onAdShowSucceeded = " + adInfo);
    }

    @Override
    public void onAdShowFailed(IronSourceError error, AdInfo adInfo) {
        Log("onAdShowFailed = " + adInfo + ": " + error);
    }

    @Override
    public void onAdClicked(AdInfo adInfo) {
        Log("onAdClicked " + adInfo);
    }

    @Override
    public void onAdClosed(AdInfo adInfo) {
        Log("onAdClosed " + adInfo);
    }

    @Override
    public void onImpressionSuccess(ImpressionData impressionData) {
        Log("onImpression: " + impressionData);
    }

    void Log(String log) {
        _activity.Log("Interstitial " + log);
    }
}
