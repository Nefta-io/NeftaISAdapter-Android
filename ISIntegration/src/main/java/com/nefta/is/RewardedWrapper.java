package com.nefta.is;

import android.app.AlertDialog;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;

import com.ironsource.adapters.custom.nefta.NeftaCustomAdapter;
import com.unity3d.mediation.LevelPlayAdError;
import com.unity3d.mediation.LevelPlayAdInfo;
import com.unity3d.mediation.rewarded.LevelPlayReward;
import com.unity3d.mediation.rewarded.LevelPlayRewardedAd;
import com.unity3d.mediation.rewarded.LevelPlayRewardedAdListener;

public class RewardedWrapper implements LevelPlayRewardedAdListener {
    private MainActivity _activity;
    private Button _loadButton;
    private Button _showButton;
    private LevelPlayRewardedAd _rewarded;
    private LevelPlayReward _reward;

    public RewardedWrapper(MainActivity activity, Button loadButton, Button showButton) {
        _activity = activity;
        _loadButton = loadButton;
        _showButton = showButton;

        _loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log("Load");
                _rewarded = new LevelPlayRewardedAd("kftiv52431x91zuk");
                _rewarded.setListener(RewardedWrapper.this);
                _rewarded.loadAd();
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
    public void onAdLoaded(@NonNull LevelPlayAdInfo adInfo) {
        Log("onAdLoaded " + adInfo);
        _showButton.setEnabled(true);

        NeftaCustomAdapter.OnExternalMediationRequestLoaded(NeftaCustomAdapter.AdType.Rewarded, 0.6, 0.7, adInfo);
    }

    @Override
    public void onAdLoadFailed(@NonNull LevelPlayAdError error) {
        Log("onAdLoadFailed: "+ error);
        _showButton.setEnabled(false);

        NeftaCustomAdapter.OnExternalMediationRequestFailed(NeftaCustomAdapter.AdType.Rewarded, 0.2, 0.3, error);
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
}
