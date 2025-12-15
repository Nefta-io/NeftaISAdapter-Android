package com.nefta.is;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ironsource.adapters.custom.nefta.NeftaCustomAdapter;
import com.nefta.sdk.AdInsight;
import com.nefta.sdk.Insights;
import com.nefta.sdk.NeftaPlugin;
import com.unity3d.mediation.LevelPlayAdError;
import com.unity3d.mediation.LevelPlayAdInfo;
import com.unity3d.mediation.rewarded.LevelPlayReward;
import com.unity3d.mediation.rewarded.LevelPlayRewardedAd;
import com.unity3d.mediation.rewarded.LevelPlayRewardedAdListener;

public class RewardedWrapper extends TableLayout {

    private enum State {
        Idle,
        LoadingWithInsights,
        Loading,
        Ready
    }

    private class AdRequest implements LevelPlayRewardedAdListener {
        public final String _adUnitId;
        public LevelPlayRewardedAd _rewarded;
        public State _state = State.Idle;
        public AdInsight _insight;
        public double _revenue;
        public int _consecutiveAdFails;

        public AdRequest(String adUnitId) {
            _adUnitId = adUnitId;
        }

        @Override
        public void onAdLoadFailed(@NonNull LevelPlayAdError error) {
            NeftaCustomAdapter.OnExternalMediationRequestFailed(error);

            Log("Load failed : "+ _adUnitId + ": "+ error);

            _rewarded = null;
            OnLoadFail();
        }

        public void OnLoadFail() {
            _consecutiveAdFails++;
            RetryLoad();

            OnTrackLoad(false);
        }

        @Override
        public void onAdLoaded(@NonNull LevelPlayAdInfo adInfo) {
            NeftaCustomAdapter.OnExternalMediationRequestLoaded(adInfo);

            Log("Loaded " + _adUnitId + " at: "+ adInfo.getRevenue());

            _insight = null;
            _consecutiveAdFails = 0;
            _revenue = adInfo.getRevenue();
            _state = State.Ready;

            OnTrackLoad(true);
        }

        private void RetryLoad() {
            _handler.postDelayed(() -> {
                _state = State.Idle;
                RetryLoading();
            }, 5000);
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

            RetryLoading();
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

            RetryLoading();
        }

        @Override
        public void onAdInfoChanged(@NonNull LevelPlayAdInfo adInfo) {
            Log("onAdInfoChanged " + adInfo);
        }
    }

    private AdRequest _adRequestA;
    private AdRequest _adRequestB;
    private boolean _isFirstResponseReceived = false;
    private LevelPlayReward _reward;

    private Activity _activity;
    private Switch _loadSwitch;
    private Button _showButton;
    private TextView _status;
    private Handler _handler;

    private void StartLoading() {
        Load(_adRequestA, _adRequestB._state);
        Load(_adRequestB, _adRequestA._state);
    }

    private void Load(AdRequest request, State otherState) {
        if (request._state == State.Idle) {
            if (otherState != State.LoadingWithInsights) {
                GetInsightsAndLoad(request);
            } else if (_isFirstResponseReceived) {
                LoadDefault(request);
            }
        }
    }

    private void GetInsightsAndLoad(AdRequest request) {
        request._state = State.LoadingWithInsights;

        NeftaPlugin._instance.GetInsights(Insights.REWARDED, request._insight, (Insights insights) -> {
            Log("LoadWithInsights: " + insights);
            if (insights._rewarded != null) {
                request._insight = insights._rewarded;
                LevelPlayRewardedAd.Config config = new LevelPlayRewardedAd.Config.Builder()
                        .setBidFloor(request._insight._floorPrice).build();
                request._rewarded = new LevelPlayRewardedAd(request._adUnitId, config);
                request._rewarded.setListener(request);

                NeftaCustomAdapter.OnExternalMediationRequest(request._rewarded, request._insight);

                Log("Loading " + request._adUnitId + " as Optimized with floor: " + request._insight._floorPrice);
                request._rewarded.loadAd();
            } else {
                request.OnLoadFail();
            }
        },5);
    }

    private void LoadDefault(AdRequest request) {
        request._state = State.Loading;

        Log("Loading "+ request._adUnitId + " as Default");

        request._rewarded = new LevelPlayRewardedAd(request._adUnitId);
        request._rewarded.setListener(request);
        request._rewarded.loadAd();

        NeftaCustomAdapter.OnExternalMediationRequest(request._rewarded);
    }

    public RewardedWrapper(Context context) {
        super(context);
        if (context instanceof Activity) {
            _activity = (Activity) context;
        }
    }

    public RewardedWrapper(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        if (context instanceof Activity) {
            _activity = (Activity) context;
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        _loadSwitch = findViewById(R.id.rewarded_load);
        _showButton = findViewById(R.id.rewarded_show);
        _status = findViewById(R.id.rewarded_status);

        _handler = new Handler(Looper.getMainLooper());

        _adRequestA = new AdRequest("x3helvrx8elhig4z");
        _adRequestB = new AdRequest("kftiv52431x91zuk");

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
                if (_adRequestA._state == State.Ready) {
                    if (_adRequestB._state == State.Ready && _adRequestB._revenue > _adRequestA._revenue) {
                        isShown = TryShow(_adRequestB);
                    }
                    if (!isShown) {
                        isShown = TryShow(_adRequestA);
                    }
                }
                if (!isShown && _adRequestB._state == State.Ready) {
                    TryShow(_adRequestB);
                }
                UpdateShowButton();
            }
        });
        _showButton.setEnabled(false);
    }

    private boolean TryShow(AdRequest request) {
        request._state = State.Idle;
        request._revenue = -1;

        if (request._rewarded.isAdReady()) {
            request._rewarded.showAd(_activity);
            return true;
        }
        RetryLoading();
        return false;
    }

    public void RetryLoading() {
        if (_loadSwitch.isChecked()) {
            StartLoading();
        }
    }

    public void OnTrackLoad(boolean success) {
        if (success) {
            UpdateShowButton();
        }

        _isFirstResponseReceived = true;
        RetryLoading();
    }

    private void UpdateShowButton() {
        _showButton.setEnabled(_adRequestA._state == State.Ready || _adRequestB._state == State.Ready);
    }

    public void OnReady() {
        Log("Ready to load..");
        _loadSwitch.setEnabled(true);
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

    private void Log(String log) {
        _status.setText(log);
        Log.i("NeftaPluginIS", "Rewarded " + log);
    }
}
