package com.nefta.is;

import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;

import com.ironsource.adapters.custom.nefta.NeftaCustomAdapter;
import com.nefta.sdk.AdInsight;
import com.nefta.sdk.Insights;
import com.nefta.sdk.NeftaPlugin;
import com.unity3d.mediation.LevelPlayAdError;
import com.unity3d.mediation.LevelPlayAdInfo;
import com.unity3d.mediation.rewarded.LevelPlayReward;
import com.unity3d.mediation.rewarded.LevelPlayRewardedAd;
import com.unity3d.mediation.rewarded.LevelPlayRewardedAdListener;

public class RewardedWrapper implements LevelPlayRewardedAdListener {

    private MainActivity _activity;
    private Button _loadButton;
    private Button _showButton;
    private Handler _handler;
    private boolean _isLoading;

    private LevelPlayRewardedAd _rewarded;
    private AdInsight _usedInsight;
    private double _requestedFloorPrice;
    private LevelPlayReward _reward;

    private void GetInsightsAndLoad() {
        NeftaPlugin._instance.GetInsights(Insights.REWARDED, this::Load, 5);
    }

    private void Load(Insights insights) {
        _requestedFloorPrice = 0;
        _usedInsight = insights._rewarded;
        if (_usedInsight != null) {
            _requestedFloorPrice = _usedInsight._floorPrice;
        }

        Log("Loading Rewarded with floor: "+ _requestedFloorPrice);

        LevelPlayRewardedAd.Config config = new LevelPlayRewardedAd.Config.Builder()
                .setBidFloor(_requestedFloorPrice).build();

        _rewarded = new LevelPlayRewardedAd("kftiv52431x91zuk", config);
        _rewarded.setListener(RewardedWrapper.this);
        _rewarded.loadAd();
    }

    @Override
    public void onAdLoadFailed(@NonNull LevelPlayAdError error) {
        NeftaCustomAdapter.OnExternalMediationRequestFailed(NeftaCustomAdapter.AdType.Rewarded, _usedInsight, _requestedFloorPrice, error);

        Log("onAdLoadFailed: "+ error);

        _handler.postDelayed(() -> {
            if (_isLoading) {
                GetInsightsAndLoad();
            }
        }, 5000);
    }

    @Override
    public void onAdLoaded(@NonNull LevelPlayAdInfo adInfo) {
        NeftaCustomAdapter.OnExternalMediationRequestLoaded(NeftaCustomAdapter.AdType.Rewarded, _usedInsight, _requestedFloorPrice, adInfo);

        Log("onAdLoaded " + adInfo);

        SetLoadingButton(false);
        _loadButton.setEnabled(false);
        _showButton.setEnabled(true);
    }

    public RewardedWrapper(MainActivity activity, Button loadButton, Button showButton) {
        _activity = activity;
        _loadButton = loadButton;
        _showButton = showButton;

        _handler = new Handler(Looper.getMainLooper());

        _loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (_isLoading) {
                    SetLoadingButton(false);
                } else {
                    Log("GetInsightsAndLoad...");
                    GetInsightsAndLoad();
                    SetLoadingButton(true);
                }
            }
        });
        _showButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (_rewarded.isAdReady()) {
                    Log("Show");
                    _rewarded.showAd(_activity);
                } else {
                    Log("Not available");
                }

                _loadButton.setEnabled(true);
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
    public void onAdDisplayed(@NonNull LevelPlayAdInfo adInfo) {
        Log("onAdDisplayed " + adInfo);
    }

    @Override
    public void onAdDisplayFailed(@NonNull LevelPlayAdError error, @NonNull LevelPlayAdInfo adInfo) {
        Log("onAdDisplayFailed " + adInfo + " : " + error);
    }

    @Override
    public void onAdClicked(@NonNull LevelPlayAdInfo adInfo) {
        Log("onAdClicked: " + adInfo);
    }

    @Override
    public void onAdRewarded(@NonNull LevelPlayReward reward, @NonNull LevelPlayAdInfo adInfo) {
        Log("onAdRewarded "+ adInfo + ": "+ reward);
        _reward = reward;
    }

    @Override
    public void onAdClosed(@NonNull LevelPlayAdInfo adInfo) {
        Log("onAdClosed " + adInfo);
        ShowRewardDialog();
    }

    @Override
    public void onAdInfoChanged(@NonNull LevelPlayAdInfo adInfo) {
        Log("onAdInfoChanged " + adInfo);
    }

    private void ShowRewardDialog() {
        if (_reward != null) {
            new AlertDialog.Builder(_activity)
                    .setPositiveButton("ok", (dialog, id) -> dialog.dismiss())
                    .setTitle("Rewarded")
                    .setMessage("Reward: " + _reward.getName() + " " + _reward.getAmount())
                    .setCancelable(false)
                    .create()
                    .show();

            _reward = null;
        }
    }

    private void Log(String message) {
        _activity.Log("Rewarded "+ message);
    }

    private void SetLoadingButton(boolean isLoading) {
        _isLoading = isLoading;
        if (isLoading) {
            _loadButton.setText("Cancel");
        } else {
            _loadButton.setText("Load Rewarded");
        }
    }
}
