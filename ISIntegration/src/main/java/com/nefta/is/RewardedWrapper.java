package com.nefta.is;

import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;

import com.ironsource.adapters.custom.nefta.NeftaCustomAdapter;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.WaterfallConfiguration;
import com.nefta.sdk.Insight;
import com.nefta.sdk.NeftaPlugin;
import com.unity3d.mediation.LevelPlayAdError;
import com.unity3d.mediation.LevelPlayAdInfo;
import com.unity3d.mediation.rewarded.LevelPlayReward;
import com.unity3d.mediation.rewarded.LevelPlayRewardedAd;
import com.unity3d.mediation.rewarded.LevelPlayRewardedAdListener;

import java.util.HashMap;

public class RewardedWrapper implements LevelPlayRewardedAdListener {

    private final String FloorPriceInsightName = "calculated_user_floor_price_rewarded";

    private double _requestedBidFoor;
    private double _calculatedBidFloor;
    private boolean _isLoadRequested;

    private MainActivity _activity;
    private Button _loadButton;
    private Button _showButton;
    private Handler _handler;

    private LevelPlayRewardedAd _rewarded;
    private LevelPlayReward _reward;

    private void GetInsightsAndLoad() {
        _isLoadRequested = true;

        NeftaPlugin._instance.GetBehaviourInsight(new String[] { FloorPriceInsightName }, this::OnBehaviourInsight);

        _handler.postDelayed(() -> {
            if (_isLoadRequested) {
                _calculatedBidFloor = 0;
                Load();
            }
        }, 5000);
    }

    private void OnBehaviourInsight(HashMap<String, Insight> insights) {
        _calculatedBidFloor = 0;
        if (insights.containsKey(FloorPriceInsightName)) {
            _calculatedBidFloor = insights.get(FloorPriceInsightName)._float;
        }

        Log("OnBehaviourInsights for Rewarded calculated bid floor: "+ _calculatedBidFloor);

        if (_isLoadRequested) {
            Load();
        }
    }

    private void Load() {
        _isLoadRequested = false;

        if (_calculatedBidFloor <= 0) {
            _requestedBidFoor = 0;
            IronSource.setWaterfallConfiguration(WaterfallConfiguration.empty(), IronSource.AD_UNIT.REWARDED_VIDEO);
        } else {
            _requestedBidFoor = _calculatedBidFloor;
            WaterfallConfiguration.WaterfallConfigurationBuilder builder = WaterfallConfiguration.builder();
            WaterfallConfiguration waterfallConfiguration = builder
                    .setFloor(_requestedBidFoor)
                    .build();
            IronSource.setWaterfallConfiguration(waterfallConfiguration, IronSource.AD_UNIT.REWARDED_VIDEO);
        }

        Log("Loading Rewarded with floor: "+ _requestedBidFoor);

        _rewarded = new LevelPlayRewardedAd("kftiv52431x91zuk");
        _rewarded.setListener(RewardedWrapper.this);
        _rewarded.loadAd();
    }

    @Override
    public void onAdLoadFailed(@NonNull LevelPlayAdError error) {
        NeftaCustomAdapter.OnExternalMediationRequestFailed(NeftaCustomAdapter.AdType.Rewarded, _requestedBidFoor, _calculatedBidFloor, error);

        Log("onAdLoadFailed: "+ error);

        _loadButton.setEnabled(true);
        _showButton.setEnabled(false);

        _handler.postDelayed(this::GetInsightsAndLoad, 5000);
    }

    @Override
    public void onAdLoaded(@NonNull LevelPlayAdInfo adInfo) {
        NeftaCustomAdapter.OnExternalMediationRequestLoaded(NeftaCustomAdapter.AdType.Rewarded, _requestedBidFoor, _calculatedBidFloor, adInfo);

        Log("onAdLoaded " + adInfo);

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
                GetInsightsAndLoad();
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
