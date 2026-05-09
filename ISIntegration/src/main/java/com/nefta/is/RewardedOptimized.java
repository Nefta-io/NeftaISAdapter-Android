package com.nefta.is;

import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;

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

public class RewardedOptimized implements Rewarded {
    private enum State {
        Idle,
        LoadingWithInsights,
        Loading,
        Ready,
        Shown
    }

    private class Track implements LevelPlayRewardedAdListener {
        public final String _adUnitId;
        public LevelPlayRewardedAd _rewarded;
        public State _state = State.Idle;
        public AdInsight _insight;
        public double _revenue;

        public Track(String adUnitId) {
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
            RetryLoad();

            OnTrackLoad(false);
        }

        @Override
        public void onAdLoaded(@NonNull LevelPlayAdInfo adInfo) {
            NeftaCustomAdapter.OnExternalMediationRequestLoaded(adInfo);

            Log("Loaded " + _adUnitId + " at: "+ adInfo.getRevenue());

            _insight = null;
            _revenue = adInfo.getRevenue();
            _state = State.Ready;

            OnTrackLoad(true);
        }

        private void RetryLoad() {
            _handler.postDelayed(() -> {
                _state = State.Idle;
                RetryLoadTracks();
            }, (int)(NeftaCustomAdapter.GetRetryDelayInSeconds(_insight) * 1000));
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

            _state = State.Idle;
            RetryLoadTracks();
        }

        @Override
        public void onAdRewarded(@NonNull LevelPlayReward reward, @NonNull LevelPlayAdInfo adInfo) {
            Log("onAdRewarded "+ adInfo + ": "+ reward);
            _reward = reward;
        }

        @Override
        public void onAdClosed(@NonNull LevelPlayAdInfo adInfo) {
            Log("onAdClosed " + adInfo);

            _state = State.Idle;
            RetryLoadTracks();

            ShowRewardDialog();
        }

        @Override
        public void onAdInfoChanged(@NonNull LevelPlayAdInfo adInfo) {
            Log("onAdInfoChanged " + adInfo);
            _revenue = adInfo.getRevenue();
        }
    }

    private Track _trackA;
    private Track _trackB;
    private boolean _isFirstResponseReceived = false;
    private LevelPlayReward _reward;

    private RewardedUi _ui;
    private Handler _handler;

    public void Init(RewardedUi ui) {
        _ui = ui;
        _handler = new Handler(Looper.getMainLooper());

        _trackA = new Track(Rewarded.AdUnitA);
        _trackB = new Track(Rewarded.AdUnitB);
    }

    public void Load() {
        LoadTrack(_trackA, _trackB._state);
        LoadTrack(_trackB, _trackA._state);
    }

    private void LoadTrack(Track track, State otherState) {
        if (track._state == State.Idle) {
            if (otherState == State.LoadingWithInsights || otherState == State.Shown) {
                if (_isFirstResponseReceived) {
                    LoadDefault(track);
                }
            } else {
                GetInsightsAndLoad(track);
            }
        }
    }

    private void GetInsightsAndLoad(Track track) {
        track._state = State.LoadingWithInsights;

        NeftaPlugin._instance.GetInsights(Insights.REWARDED, track._insight, (Insights insights) -> {
            Log("LoadWithInsights: " + insights);
            if (insights._rewarded != null) {
                track._insight = insights._rewarded;
                if (track._insight._floorPrice >= 0) {
                    LevelPlayRewardedAd.Config config = new LevelPlayRewardedAd.Config.Builder()
                            .setBidFloor(track._insight._floorPrice).build();
                    track._rewarded = new LevelPlayRewardedAd(track._adUnitId, config);
                } else {
                    track._rewarded = new LevelPlayRewardedAd(track._adUnitId);
                }
                track._rewarded.setListener(track);

                NeftaCustomAdapter.OnExternalMediationRequest(track._rewarded, track._insight);

                Log("Loading " + track._adUnitId + " as Optimized with floor: " + track._insight._floorPrice);
                track._rewarded.loadAd();
            } else {
                track.OnLoadFail();
            }
        });
    }

    private void LoadDefault(Track track) {
        track._state = State.Loading;

        Log("Loading "+ track._adUnitId + " as Default");

        track._rewarded = new LevelPlayRewardedAd(track._adUnitId);
        track._rewarded.setListener(track);

        NeftaCustomAdapter.OnExternalMediationRequest(track._rewarded);

        track._rewarded.loadAd();
    }

    public void Show() {
        boolean isShown = false;
        if (_trackA._state == State.Ready) {
            if (_trackB._state == State.Ready && _trackB._revenue > _trackA._revenue) {
                isShown = TryShow(_trackB);
            }
            if (!isShown) {
                isShown = TryShow(_trackA);
            }
        }
        if (!isShown && _trackB._state == State.Ready) {
            TryShow(_trackB);
        }
        UpdateAvailability();
    }

    private boolean TryShow(Track request) {
        request._revenue = -1;
        if (request._rewarded.isAdReady()) {
            request._state = State.Shown;
            request._rewarded.showAd(_ui.Activity);
            return true;
        }
        request._state = State.Idle;
        RetryLoadTracks();
        return false;
    }

    public void RetryLoadTracks() {
        if (_ui.IsAutoLoad) {
            Load();
        }
    }

    public void OnTrackLoad(boolean success) {
        if (success) {
            UpdateAvailability();
        }

        _isFirstResponseReceived = true;
        RetryLoadTracks();
    }

    private void UpdateAvailability() {
        _ui.SetAvailability(_trackA._state == State.Ready || _trackB._state == State.Ready);
    }

    private void Log(String log) {
        _ui.Log(log);
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
}
