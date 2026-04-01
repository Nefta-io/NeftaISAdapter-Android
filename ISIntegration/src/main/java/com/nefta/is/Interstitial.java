package com.nefta.is;

import android.app.Activity;
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
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAd;
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAdListener;

public class Interstitial extends TableLayout {

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
        }
    }

    private Track _trackA;
    private Track _trackB;
    private boolean _isFirstResponseReceived = false;

    private Activity _activity;
    private Switch _loadSwitch;
    private Button _showButton;
    private TextView _status;
    private Handler _handler;

    private void LoadTracks() {
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
                LevelPlayInterstitialAd.Config config = new LevelPlayInterstitialAd.Config.Builder()
                        .setBidFloor(track._insight._floorPrice).build();
                track._interstitial = new LevelPlayInterstitialAd(track._adUnitId, config);
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

    public Interstitial(Context context) {
        super(context);
        if (context instanceof Activity) {
            _activity = (Activity) context;
        }
    }

    public Interstitial(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        if (context instanceof Activity) {
            _activity = (Activity) context;
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        _loadSwitch = findViewById(R.id.interstitial_load);
        _showButton = findViewById(R.id.interstitial_show);
        _status = findViewById(R.id.interstitial_status);

        _handler = new Handler(Looper.getMainLooper());

        _trackA = new Track("0u6jgm23ggqso85n");
        _trackB = new Track("wrzl86if1sqfxquc");

        _loadSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    LoadTracks();
                }
            }
        });
        _showButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                UpdateShowButton();
            }
        });
        _showButton.setEnabled(false);
    }

    private boolean TryShow(Track request) {
        request._revenue = -1;
        if (request._interstitial.isAdReady()) {
            request._state = State.Shown;
            request._interstitial.showAd(_activity);
           return true;
        }
        request._state = State.Idle;
        RetryLoadTracks();
        return false;
    }

    public void RetryLoadTracks() {
        if (_loadSwitch.isChecked()) {
            LoadTracks();
        }
    }

    public void OnTrackLoad(boolean success) {
        if (success) {
            UpdateShowButton();
        }

        _isFirstResponseReceived = true;
        RetryLoadTracks();
    }

    private void UpdateShowButton() {
        _showButton.setEnabled(_trackA._state == State.Ready || _trackB._state == State.Ready);
    }

    private void Log(String log) {
        _status.setText(log);
        Log.i("NeftaPluginIS", "Interstitial " + log);
    }
}
