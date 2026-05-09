package com.nefta.is;

import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.ironsource.adapters.custom.nefta.NeftaCustomAdapter;
import com.unity3d.mediation.LevelPlayAdError;
import com.unity3d.mediation.LevelPlayAdInfo;
import com.unity3d.mediation.rewarded.LevelPlayReward;
import com.unity3d.mediation.rewarded.LevelPlayRewardedAd;
import com.unity3d.mediation.rewarded.LevelPlayRewardedAdListener;

public class RewardedDefault implements LevelPlayRewardedAdListener, Rewarded {

    private RewardedUi _ui;
    private LevelPlayRewardedAd _rewarded;
    private LevelPlayReward _reward;
    private Handler _handler;

    public void Init(RewardedUi ui) {
        _ui = ui;
        _handler = new Handler(Looper.getMainLooper());

        _rewarded = new LevelPlayRewardedAd(Rewarded.AdUnitA);
        _rewarded.setListener(this);
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
    public void onAdDisplayed(@NonNull LevelPlayAdInfo adInfo) {
        Log("onAdDisplayed " + adInfo);
    }

    @Override
    public void onAdDisplayFailed(@NonNull LevelPlayAdError error, @NonNull LevelPlayAdInfo adInfo) {
        Log("onAdDisplayFailed " + adInfo + " : " + error);

        if (_ui.IsAutoLoad) {
            Load();
        }
    }

    @Override
    public void onAdRewarded(@NonNull LevelPlayReward reward, @NonNull LevelPlayAdInfo adInfo) {
        Log("onAdRewarded "+ adInfo + ": "+ reward);

        _reward = reward;
    }

    @Override
    public void onAdClosed(@NonNull LevelPlayAdInfo adInfo) {
        Log("onAdClosed " + adInfo);

        if (_ui.IsAutoLoad) {
            Load();
        }

        ShowRewardDialog();
    }

    public void Load() {
        NeftaCustomAdapter.OnExternalMediationRequest(_rewarded);
        _rewarded.loadAd();
    }

    public void Show() {
        if (_rewarded.isAdReady()) {
            _rewarded.showAd(_ui.Activity);
        } else if (_ui.IsAutoLoad) {
            Load();
        }

        _ui.SetAvailability(false);
    }

    private void ShowRewardDialog() {
        if (_reward != null) {
            new AlertDialog.Builder(_ui.Activity)
                    .setPositiveButton("ok", (dialog, id) -> dialog.dismiss())
                    .setTitle("Rewarded")
                    .setMessage("Reward: " + _reward.getName() + " " + _reward.getAmount())
                    .setCancelable(false)
                    .create()
                    .show();

            _reward = null;
        }
    }

    private void Log(String log) {
        _ui.Log(log);
    }
}
