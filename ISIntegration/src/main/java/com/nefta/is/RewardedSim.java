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
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ironsource.adapters.custom.nefta.NeftaCustomAdapter;
import com.ironsource.em;
import com.ironsource.mediationsdk.impressionData.ImpressionData;
import com.nefta.sdk.AdInsight;
import com.nefta.sdk.Insights;
import com.nefta.sdk.NeftaPlugin;
import com.unity3d.mediation.LevelPlayAdError;
import com.unity3d.mediation.LevelPlayAdInfo;
import com.unity3d.mediation.LevelPlayAdSize;
import com.unity3d.mediation.impression.LevelPlayImpressionData;
import com.unity3d.mediation.rewarded.LevelPlayReward;
import com.unity3d.mediation.rewarded.LevelPlayRewardedAd;
import com.unity3d.mediation.rewarded.LevelPlayRewardedAdListener;

import org.json.JSONException;
import org.json.JSONObject;

public class RewardedSim extends TableLayout {

    private enum State {
        Idle,
        LoadingWithInsights,
        Loading,
        Ready
    }

    private class AdRequest implements LevelPlayRewardedAdListener {
        public final String _adUnitId;
        public SLevelPlayRewardedAd _rewarded;
        public State _state = State.Idle;
        public AdInsight _insight;
        public double _revenue;
        public int _consecutiveAdFails;

        public AdRequest(String adUnitId) {
            _adUnitId = adUnitId;
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
        public void onAdDisplayFailed(@NonNull LevelPlayAdError error, @NonNull LevelPlayAdInfo info) {
            Log("onAdDisplayFailed = " + info + ": " + error);

            RetryLoading();
        }

        @Override
        public void onAdRewarded(@NonNull LevelPlayReward reward, @NonNull LevelPlayAdInfo adInfo) {
            Log("onAdRewarded "+ adInfo + ": "+ reward);
            _reward = reward;
        }

        @Override
        public void onAdDisplayed(@NonNull LevelPlayAdInfo adInfo) {
            Log("onAdDisplayed " + adInfo);
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

    private TextView _aStatus;
    private Button _aFill2;
    private Button _aFill1;
    private Button _aNoFill;
    private Button _aOther;

    private TextView _bStatus;
    private Button _bFill2;
    private Button _bFill1;
    private Button _bNoFill;
    private Button _bOther;

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
                request._rewarded = new SLevelPlayRewardedAd(request._adUnitId, config);
                request._rewarded.setListener(request);

                NeftaCustomAdapter.OnExternalMediationRequest(request._rewarded._i, request._insight);

                Log("Loading "+ request._adUnitId + " as Optimized with floor: " + request._insight._floorPrice);
                request._rewarded.loadAd();
            } else {
                request.OnLoadFail();
            }
        }, 5);
    }

    private void LoadDefault(AdRequest request) {
        request._state = State.Loading;

        Log("Loading "+ request._adUnitId + " as Default");

        request._rewarded = new SLevelPlayRewardedAd(request._adUnitId);
        request._rewarded.setListener(request);

        NeftaCustomAdapter.OnExternalMediationRequest(request._rewarded._i);

        request._rewarded.loadAd();
    }

    public RewardedSim(Context context) {
        super(context);
        if (context instanceof Activity) {
            _activity = (Activity) context;
        }
    }

    public RewardedSim(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        if (context instanceof Activity) {
            _activity = (Activity) context;
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        _loadSwitch = findViewById(R.id.rewardedSim_load);
        _showButton = findViewById(R.id.rewardedSim_show);
        _status = findViewById(R.id.rewardedSim_status);

        _handler = new Handler(Looper.getMainLooper());

        _adRequestA = new AdRequest("Track A");
        _adRequestB = new AdRequest("Track B");

        _handler = new Handler(Looper.getMainLooper());

        _loadSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                StartLoading();
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

        _aStatus = findViewById(R.id.rewardedSim_statusA);
        _aFill2 = findViewById(R.id.rewardedSim_fill2A);
        _aFill2.setOnClickListener(v -> SimOnAdLoadedEvent(_adRequestA, true));
        _aFill1 = findViewById(R.id.rewardedSim_fill1A);
        _aFill1.setOnClickListener(v -> SimOnAdLoadedEvent(_adRequestA, false));
        _aNoFill = findViewById(R.id.rewardedSim_noFillA);
        _aNoFill.setOnClickListener(v -> SimOnAdFailedEvent(_adRequestA, 2));
        _aOther = findViewById(R.id.rewardedSim_OtherA);
        _aOther.setOnClickListener(v -> SimOnAdFailedEvent(_adRequestA, 0));
        ToggleTrackA(false, true);

        _bStatus = findViewById(R.id.rewardedSim_statusB);
        _bFill2 = findViewById(R.id.rewardedSim_fill2B);
        _bFill2.setOnClickListener(v -> SimOnAdLoadedEvent(_adRequestB, true));
        _bFill1 = findViewById(R.id.rewardedSim_fill1B);
        _bFill1.setOnClickListener(v -> SimOnAdLoadedEvent(_adRequestB, false));
        _bNoFill = findViewById(R.id.rewardedSim_noFillB);
        _bNoFill.setOnClickListener(v -> SimOnAdFailedEvent(_adRequestB, 2));
        _bOther = findViewById(R.id.rewardedSim_OtherB);
        _bOther.setOnClickListener(v -> SimOnAdFailedEvent(_adRequestB, 0));
        ToggleTrackB(false, true);
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

    private void Log(String log) {
        _status.setText(log);
        Log.i("NeftaPluginIS", "Rewarded " + log);
    }

    private class SLevelPlayRewardedAd {
        public String _adUnitId;
        public LevelPlayRewardedAd _i;
        public LevelPlayAdInfo _adInfo;
        public double _floor = -1;
        public LevelPlayRewardedAdListener _listener;
        public LevelPlayImpressionData _impressionData;

        public SLevelPlayRewardedAd(String adUnitId) {
            this(adUnitId, null);
        }

        public SLevelPlayRewardedAd(String adUnitId, LevelPlayRewardedAd.Config config) {
            _adUnitId = adUnitId;

            _i = new LevelPlayRewardedAd(adUnitId);

            if (config != null) {
                _floor = config.getBidFloor();
            }
        }

        public void setListener(LevelPlayRewardedAdListener listener) {
            _listener = listener;
        }

        public void loadAd() {
            String status = _adUnitId + " loading " + (_floor >= 0 ? " as Optimized" : "as Default");

            if (_adRequestA._adUnitId.equals(_adUnitId)) {
                ToggleTrackA(true, true);
                _aStatus.setText(status);
            } else {
                ToggleTrackB(true, true);
                _bStatus.setText(status);
            }
        }

        public void showAd(Activity activity) {
            NeftaCustomAdapter.OnExternalMediationImpression(_impressionData);

            SimulatorAd.Instance.Show("Rewarded",
                    () -> { _listener.onAdDisplayed(_adInfo); },
                    () -> { _listener.onAdClicked(_adInfo); },
                    () -> { _listener.onAdRewarded(new LevelPlayReward("sim reward", 1), _adInfo);},
                    () -> {
                        _listener.onAdClosed(_adInfo);
                        _adInfo = null;
                    }
            );

            if (_adRequestA._adUnitId.equals(_adUnitId)) {
                _aStatus.setText("Showing A");
            } else {
                _bStatus.setText("Showing B");
            }
        }

        public void SimLoad(LevelPlayAdInfo ad) {
            _adInfo = ad;
            _listener.onAdLoaded(_adInfo);
        }

        public void SimFailLoad(LevelPlayAdError error) {
            _listener.onAdLoadFailed(error);
        }

        public boolean isAdReady() {
            return _adInfo != null;
        }
    }

    private void ToggleTrackA(boolean on, boolean refresh) {
        _aFill2.setEnabled(on);
        _aFill1.setEnabled(on);
        _aNoFill.setEnabled(on);
        _aOther.setEnabled(on);

        if (refresh) {
            _aFill2.setBackgroundResource(R.drawable.button);
            _aFill1.setBackgroundResource(R.drawable.button);
            _aNoFill.setBackgroundResource(R.drawable.button);
            _aOther.setBackgroundResource(R.drawable.button);
        }

        _aFill2.refreshDrawableState();
        _aFill1.refreshDrawableState();
        _aNoFill.refreshDrawableState();
        _aOther.refreshDrawableState();
    }

    private void ToggleTrackB(boolean on, boolean refresh) {
        _bFill2.setEnabled(on);
        _bFill1.setEnabled(on);
        _bNoFill.setEnabled(on);
        _bOther.setEnabled(on);

        if (refresh) {
            _bFill2.setBackgroundResource(R.drawable.button);
            _bFill1.setBackgroundResource(R.drawable.button);
            _bNoFill.setBackgroundResource(R.drawable.button);
            _bOther.setBackgroundResource(R.drawable.button);
        }

        _bFill2.refreshDrawableState();
        _bFill1.refreshDrawableState();
        _bNoFill.refreshDrawableState();
        _bOther.refreshDrawableState();
    }

    private void SimOnAdLoadedEvent(AdRequest request, boolean isHigh) {
        double revenue = isHigh ? 0.002 : 0.001;
        if (request._rewarded._adInfo != null) {
            request._rewarded._adInfo = null;

            if (request == _adRequestA) {
                if (isHigh) {
                    _aFill2.setBackgroundResource(R.drawable.button);
                    _aFill2.setEnabled(false);
                } else {
                    _aFill1.setBackgroundResource(R.drawable.button);
                    _aFill1.setEnabled(false);
                }
            } else {
                if (isHigh) {
                    _bFill2.setBackgroundResource(R.drawable.button);
                    _bFill2.setEnabled(false);
                } else {
                    _bFill1.setBackgroundResource(R.drawable.button);
                    _bFill1.setEnabled(false);
                }
            }
            return;
        }

        String auctionId = java.util.UUID.randomUUID().toString();
        String adFormat = "rewarded_video";
        String precision = "BID";
        JSONObject impressionData = new JSONObject();
        try {
            impressionData.put("auctionId", auctionId);
            impressionData.put("mediationAdUnitId", request._adUnitId);
            impressionData.put("mediationAdUnitName", "");
            impressionData.put("adFormat", adFormat);
            impressionData.put("instanceName", "");
            impressionData.put("instanceId", "");
            impressionData.put("country", "");
            impressionData.put("placement", "");
            impressionData.put("revenue", revenue);
            impressionData.put("precision", precision);
            impressionData.put("creativeId", "simulator creative "+ auctionId);
        } catch (JSONException e) {

        }
        LevelPlayAdInfo adInfo = new LevelPlayAdInfo(
                request._rewarded._i.getAdId(),
                request._adUnitId,
                adFormat,
                new ImpressionData(impressionData),
                new em(revenue, precision),
                new LevelPlayAdSize(10, 10, null, false, null),
                "placement name");


        request._rewarded._impressionData = new LevelPlayImpressionData(impressionData);

        if (request == _adRequestA) {
            ToggleTrackA(false, false);
            if (isHigh) {
                _aFill2.setBackgroundResource(R.drawable.button_fill);
                _aFill2.setEnabled(true);
            } else {
                _aFill1.setBackgroundResource(R.drawable.button_fill);
                _aFill1.setEnabled(true);
            }
            _aStatus.setText(request._adUnitId + " loaded " + revenue);
        } else {
            ToggleTrackB(false, false);
            if (isHigh) {
                _bFill2.setBackgroundResource(R.drawable.button_fill);
                _bFill2.setEnabled(true);
            } else {
                _bFill1.setBackgroundResource(R.drawable.button_fill);
                _bFill1.setEnabled(true);
            }
            _bStatus.setText(request._adUnitId + " loaded " + revenue);
        }

        request._rewarded.SimLoad(adInfo);
    }

    private void SimOnAdFailedEvent(AdRequest request, int status) {
        if (request == _adRequestA) {
            if (status == 2) {
                _aNoFill.setBackgroundResource(R.drawable.button_no);
            } else {
                _aOther.setBackgroundResource(R.drawable.button_no);
            }
            ToggleTrackA(false, false);
            _aStatus.setText(request._adUnitId + " failed");
        } else {
            if (status == 2) {
                _bNoFill.setBackgroundResource(R.drawable.button_no);
            } else {
                _bOther.setBackgroundResource(R.drawable.button_no);
            }
            _bStatus.setText(request._adUnitId + " failed");
            ToggleTrackB(false, false);
        }

        int errorCode = status == 2 ? 509 : 0;
        LevelPlayAdError error = new LevelPlayAdError(request._rewarded._i.getAdId(), request._adUnitId, errorCode, "error");
        request._rewarded.SimFailLoad(error);
    }
}
