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
    private double _dynamicAdRevenue = -1;
    private AdInsight _dynamicInsight;
    private LevelPlayRewardedAd _defaultRewarded;
    private double _defaultAdRevenue = -1;
    private LevelPlayReward _reward;

    private MainActivity _activity;
    private final Switch _loadSwitch;
    private Button _showButton;
    private Handler _handler;

    private void StartLoading() {
        if (_dynamicRewarded == null) {
            GetInsightsAndLoad(null);
        }
        if (_defaultRewarded == null) {
            LoadDefault();
        }
    }

    private void GetInsightsAndLoad(AdInsight previousInsight) {
        NeftaPlugin._instance.GetInsights(Insights.REWARDED, previousInsight, this::LoadWithInsights, 5);
    }

    private void LoadWithInsights(Insights insights) {
        _dynamicInsight = insights._rewarded;
        if (_dynamicInsight != null) {
            Log("Loading Dynamic with floor: " + _dynamicInsight._floorPrice);

            LevelPlayRewardedAd.Config config = new LevelPlayRewardedAd.Config.Builder()
                    .setBidFloor(_dynamicInsight._floorPrice).build();
            _dynamicRewarded = new LevelPlayRewardedAd(_dynamicAdUnitId, config);
            _dynamicRewarded.setListener(this);
            _dynamicRewarded.loadAd();

            NeftaCustomAdapter.OnExternalMediationRequest(_dynamicRewarded, _dynamicInsight);
        }
    }

    private void LoadDefault() {
        Log("Loading Default");

        _defaultRewarded = new LevelPlayRewardedAd(_defaultAdUnitId);
        _defaultRewarded.setListener(this);
        _defaultRewarded.loadAd();

        NeftaCustomAdapter.OnExternalMediationRequest(_defaultRewarded);
    }

    @Override
    public void onAdLoadFailed(@NonNull LevelPlayAdError error) {
        NeftaCustomAdapter.OnExternalMediationRequestFailed(error);

        if (_dynamicRewarded != null && _dynamicRewarded.getAdId().equals(error.getAdId())) {
            Log("onAdLoadFailed Dynamic: "+ error);

            _dynamicRewarded = null;
            _handler.postDelayed(() -> {
                if (_loadSwitch.isChecked()) {
                    GetInsightsAndLoad(_dynamicInsight);
                }
            }, 5000);
        } else {
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
        NeftaCustomAdapter.OnExternalMediationRequestLoaded(adInfo);
        if (_dynamicRewarded != null && _dynamicRewarded.getAdId().equals(adInfo.getAdId())) {
            Log("onAdLoaded Dynamic " + adInfo);

            _dynamicAdRevenue = adInfo.getRevenue();
        } else {
            Log("onAdLoaded Default " + adInfo);

            _defaultAdRevenue = adInfo.getRevenue();
        }

        UpdateShowButton();
    }

    @Override
    public void onAdClicked(@NonNull LevelPlayAdInfo adInfo) {
        NeftaCustomAdapter.OnExternalMediationClick(adInfo);

        Log("onAdClicked " + adInfo);
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
                if (_dynamicAdRevenue >= 0) {
                    if (_defaultAdRevenue > _dynamicAdRevenue) {
                        isShown = TryShowDefault();
                    }
                    if (!isShown) {
                        isShown = TryShowDynamic();
                    }
                }
                if (!isShown && _defaultAdRevenue >= 0) {
                    TryShowDefault();
                }
                UpdateShowButton();
            }
        });

        _loadSwitch.setEnabled(false);
        _showButton.setEnabled(false);
    }

    private boolean TryShowDynamic() {
        boolean isShown = false;
        if (_dynamicRewarded.isAdReady()) {
            _dynamicRewarded.showAd(_activity);
            isShown = true;
        }
        _dynamicAdRevenue = -1;
        _dynamicRewarded = null;
        return isShown;
    }

    private boolean TryShowDefault() {
        boolean isShown = false;
        if (_defaultRewarded.isAdReady()) {
            _defaultRewarded.showAd(_activity);
            isShown = true;
        }
        _defaultAdRevenue = -1;
        _defaultRewarded = null;
        return isShown;
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
        _showButton.setEnabled(_dynamicAdRevenue >= 0 || _defaultAdRevenue >= 0);
    }

    private void Log(String message) {
        _activity.Log("Rewarded "+ message);
    }
}
