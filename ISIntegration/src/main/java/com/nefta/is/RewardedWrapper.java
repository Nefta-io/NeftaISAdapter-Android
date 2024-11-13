package com.nefta.is;

import android.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo;
import com.ironsource.mediationsdk.impressionData.ImpressionData;
import com.ironsource.mediationsdk.impressionData.ImpressionDataListener;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.model.Placement;
import com.ironsource.mediationsdk.sdk.LevelPlayRewardedVideoManualListener;

public class RewardedWrapper implements LevelPlayRewardedVideoManualListener, ImpressionDataListener {
    private final String TAG = "REWARDED";
    private MainActivity _activity;
    private Button _loadButton;
    private Button _showButton;
    private Placement _rewardedVideoPlacementInfo;

    public RewardedWrapper(MainActivity activity, Button loadButton, Button showButton) {
        _activity = activity;
        _loadButton = loadButton;
        _showButton = showButton;

        IronSource.addImpressionDataListener(this);
        IronSource.setLevelPlayRewardedVideoManualListener(RewardedWrapper.this);

        _loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log("Load");
                IronSource.loadRewardedVideo();
            }
        });
        _showButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (IronSource.isRewardedVideoAvailable()) {
                    Log("Show");
                    IronSource.showRewardedVideo();
                } else {
                    Log("Not available");
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
    public void onAdLoadFailed(IronSourceError ironSourceError) {
        Log("onAdLoadFailed");
        _showButton.setEnabled(false);
    }

    @Override
    public void onAdOpened(AdInfo adInfo) {
        Log("onAdOpened " + adInfo);
    }

    @Override
    public void onAdShowFailed(IronSourceError error, AdInfo adInfo) {
        Log("onAdShowFailed " + adInfo + " : " + error);
    }

    @Override
    public void onAdClicked(Placement placement, AdInfo adInfo) {
        Log("onAdClicked = " + placement + ": " + adInfo);
    }

    @Override
    public void onAdRewarded(Placement placement, AdInfo adInfo) {
        Log("onAdRewarded = " + placement + ": = " + adInfo);
        _rewardedVideoPlacementInfo = placement;
    }

    @Override
    public void onAdClosed(AdInfo adInfo) {
        Log("onAdClosed " + adInfo);
        ShowRewardDialog();
    }

    @Override
    public void onImpressionSuccess(ImpressionData impressionData) {
        Log.i(TAG, "onImpression: " + impressionData);
    }

    private void ShowRewardDialog() {
        if (_rewardedVideoPlacementInfo != null) {
            new AlertDialog.Builder(_activity)
                    .setPositiveButton("ok", (dialog, id) -> dialog.dismiss())
                    .setTitle("Rewarded")
                    .setMessage("Reward: " + _rewardedVideoPlacementInfo.getRewardAmount() + " " + _rewardedVideoPlacementInfo.getRewardName())
                    .setCancelable(false)
                    .create()
                    .show();

            _rewardedVideoPlacementInfo = null;
        }
    }

    private void Log(String message) {
        _activity.Log("Rewarded "+ message);
    }
}
