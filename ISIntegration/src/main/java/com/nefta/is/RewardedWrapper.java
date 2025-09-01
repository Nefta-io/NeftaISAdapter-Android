package com.nefta.is;

import android.app.AlertDialog;
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
import com.unity3d.mediation.rewarded.LevelPlayReward;
import com.unity3d.mediation.rewarded.LevelPlayRewardedAd;
import com.unity3d.mediation.rewarded.LevelPlayRewardedAdListener;

public class RewardedWrapper implements LevelPlayRewardedAdListener {

    private static final String _dynamicAdUnitId = "x3helvrx8elhig4z";
    private static final String _defaultAdUnitId = "kftiv52431x91zuk";

    private LevelPlayRewardedAd _dynamicRewarded;
    private boolean _isDynamicLoaded;
    private AdInsight _dynamicAdUnitInsight;
    private LevelPlayRewardedAd _defaultRewarded;
    private boolean _isDefaultLoaded;
    private LevelPlayReward _reward;

    private MainActivity _activity;
    private final Switch _loadSwitch;
    private final Button _showButton;
    private final Handler _handler;

    private void StartLoading() {
        if (_dynamicRewarded == null) {
            GetInsightsAndLoad();
        }
        if (_defaultRewarded == null) {
            LoadDefault();
        }
    }

    private void GetInsightsAndLoad() {
        NeftaPlugin._instance.GetInsights(Insights.REWARDED, this::LoadWithInsights, 5);
    }

    private void LoadWithInsights(Insights insights) {
        _dynamicAdUnitInsight = insights._rewarded;
        if (_dynamicAdUnitInsight != null) {
            Log("Loading Dynamic Rewarded with floor: " + _dynamicAdUnitInsight._floorPrice);

            LevelPlayRewardedAd.Config config = new LevelPlayRewardedAd.Config.Builder()
                    .setBidFloor(_dynamicAdUnitInsight._floorPrice).build();
            _dynamicRewarded = new LevelPlayRewardedAd(_dynamicAdUnitId, config);
            _dynamicRewarded.setListener(this);
            _dynamicRewarded.loadAd();
        }
    }

    private void LoadDefault() {
        Log("Loading Default Rewarded");

        _defaultRewarded = new LevelPlayRewardedAd(_defaultAdUnitId);
        _defaultRewarded.setListener(this);
        _defaultRewarded.loadAd();
    }

    @Override
    public void onAdLoadFailed(@NonNull LevelPlayAdError error) {
        if (_dynamicAdUnitId.equals(error.getAdUnitId())) {
            NeftaCustomAdapter.OnExternalMediationRequestFailed(NeftaCustomAdapter.AdType.Rewarded, _dynamicAdUnitInsight, _dynamicAdUnitInsight._floorPrice, error);

            Log("onAdLoadFailed Dynamic: "+ error);

            _dynamicRewarded = null;
            _handler.postDelayed(() -> {
                if (_loadSwitch.isChecked()) {
                    GetInsightsAndLoad();
                }
            }, 5000);
        } else {
            NeftaCustomAdapter.OnExternalMediationRequestFailed(NeftaCustomAdapter.AdType.Rewarded, null, 0, error);

            Log("onAdLoadFailed Default: "+ error);

            _defaultRewarded = null;
            _handler.postDelayed(() -> {
                if (_loadSwitch.isChecked()) {
                    LoadDefault();
                }
            }, 5000);
        }
    }

    @Override
    public void onAdLoaded(@NonNull LevelPlayAdInfo adInfo) {
        if (_dynamicAdUnitId.equals(adInfo.getAdUnitId())) {
            NeftaCustomAdapter.OnExternalMediationRequestLoaded(NeftaCustomAdapter.AdType.Rewarded, _dynamicAdUnitInsight, _dynamicAdUnitInsight._floorPrice, adInfo);

            Log("onAdLoaded Dynamic " + adInfo);

            _isDynamicLoaded = true;
        } else {
            NeftaCustomAdapter.OnExternalMediationRequestLoaded(NeftaCustomAdapter.AdType.Rewarded,null, 0, adInfo);

            Log("onAdLoaded Default " + adInfo);

            _isDefaultLoaded = true;
        }

        UpdateShowButton();
    }

    public RewardedWrapper(MainActivity activity, Switch loadButton, Button showButton) {
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
                if (_isDynamicLoaded) {
                    if (_dynamicRewarded.isAdReady()) {
                        _dynamicRewarded.showAd(_activity);
                        isShown = true;
                    }
                    _isDynamicLoaded = false;
                    _dynamicRewarded = null;
                }
                if (!isShown && _isDefaultLoaded) {
                    Log("Show: "+ _defaultRewarded);
                    if (_defaultRewarded.isAdReady()) {
                        _defaultRewarded.showAd(_activity);
                    }
                    _isDefaultLoaded = false;
                    _defaultRewarded = null;
                }

                UpdateShowButton();
            }
        });

        _loadSwitch.setEnabled(false);
        _showButton.setEnabled(false);
    }

    public void OnReady() {
        _loadSwitch.setEnabled(true);
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

        // start new load cycle
        if (_loadSwitch.isChecked()) {
            StartLoading();
        }
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

    private void UpdateShowButton() {
        _showButton.setEnabled(_isDynamicLoaded || _isDefaultLoaded);
    }

    private void Log(String message) {
        _activity.Log("Rewarded "+ message);
    }
}
