package com.nefta.is;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.ironsource.adapters.custom.nefta.NeftaCustomAdapter;
import com.nefta.sdk.AdInsight;
import com.nefta.sdk.Insights;
import com.nefta.sdk.NeftaPlugin;
import com.unity3d.mediation.LevelPlayAdError;
import com.unity3d.mediation.LevelPlayAdInfo;
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAd;
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAdListener;

public class InterstitialOptimized implements Interstitial {
    private enum State {
        Idle,
        LoadingWithInsights,
        Loading,
        Ready,
        Shown
    }

    private class Track implements LevelPlayInterstitialAdListener {
        public final String _adUnitId;
        public LevelPlayInterstitialAd _interstitial;
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

            _interstitial = null;
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
        public void onAdDisplayFailed(@NonNull LevelPlayAdError error, @NonNull LevelPlayAdInfo info) {
            Log("onAdDisplayFailed = " + info + ": " + error);

            _state = State.Idle;
            RetryLoadTracks();
        }

        @Override
        public void onAdDisplayed(@NonNull LevelPlayAdInfo adInfo) {
            Log("onAdDisplayed " + adInfo);
        }

        @Override
        public void onAdClosed(@NonNull LevelPlayAdInfo adInfo) {
            Log("onAdClosed " + adInfo);

            _state = State.Idle;
            RetryLoadTracks();
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

    private InterstitialUi _ui;
    private Handler _handler;

    public void Init(InterstitialUi ui) {
        _ui = ui;
        _handler = new Handler(Looper.getMainLooper());

        _trackA = new Track(Interstitial.AdUnitA);
        _trackB = new Track(Interstitial.AdUnitB);
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

        NeftaPlugin._instance.GetInsights(Insights.INTERSTITIAL, track._insight, (Insights insights) -> {
            Log("LoadWithInsights: " + insights);
            if (insights._interstitial != null) {
                track._insight = insights._interstitial;
                if (track._insight._floorPrice >= 0) {
                    LevelPlayInterstitialAd.Config config = new LevelPlayInterstitialAd.Config.Builder()
                            .setBidFloor(track._insight._floorPrice).build();
                    track._interstitial = new LevelPlayInterstitialAd(track._adUnitId, config);
                } else {
                    track._interstitial = new LevelPlayInterstitialAd(track._adUnitId);
                }
                track._interstitial.setListener(track);

                NeftaCustomAdapter.OnExternalMediationRequest(track._interstitial, track._insight);

                Log("Loading "+ track._adUnitId + " as Optimized with floor: " + track._insight._floorPrice);
                track._interstitial.loadAd();
            } else {
                track.OnLoadFail();
            }
        });
    }

    private void LoadDefault(Track track) {
        track._state = State.Loading;

        Log("Loading "+ track._adUnitId + " as Default");

        track._interstitial = new LevelPlayInterstitialAd(track._adUnitId);
        track._interstitial.setListener(track);

        NeftaCustomAdapter.OnExternalMediationRequest(track._interstitial);

        track._interstitial.loadAd();
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
        if (request._interstitial.isAdReady()) {
            request._state = State.Shown;
            request._interstitial.showAd(_ui.Activity);
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
}