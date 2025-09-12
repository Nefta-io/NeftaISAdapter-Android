package com.nefta.is;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.nefta.sdk.NeftaPlugin;
import com.ironsource.adapters.custom.nefta.NeftaCustomAdapter;
import com.unity3d.mediation.LevelPlay;
import com.unity3d.mediation.LevelPlayConfiguration;
import com.unity3d.mediation.LevelPlayInitError;
import com.unity3d.mediation.LevelPlayInitListener;
import com.unity3d.mediation.LevelPlayInitRequest;
import com.unity3d.mediation.impression.LevelPlayImpressionData;
import com.unity3d.mediation.impression.LevelPlayImpressionDataListener;
import com.unity3d.mediation.segment.LevelPlaySegment;

public class MainActivity extends AppCompatActivity implements LevelPlayImpressionDataListener {

    private final String _tag = "NeftaPluginIS";
    private final String _appId = "1bb635bc5";

    private BannerWrapper _banner;
    private InterstitialWrapper _interstitial;
    private RewardedWrapper _rewarded;
    private TextView _status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        NeftaPlugin.EnableLogging(true);
        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            String override = intent.getStringExtra("override");
            if (override != null && override.length() > 2) {
                NeftaPlugin.SetOverride(override);
            }
        }

        NeftaCustomAdapter.Init(MainActivity.this, "5658160027140096");

        _status = findViewById(R.id.status);

        _banner = new BannerWrapper(this, findViewById(R.id.bannerView), findViewById(R.id.showBanner), findViewById(R.id.closeBanner));
        _interstitial = new InterstitialWrapper(this, findViewById(R.id.loadInterstitial), findViewById(R.id.showInterstitial));
        _rewarded = new RewardedWrapper(this, findViewById(R.id.loadRewarded), findViewById(R.id.showRewarded));

        LevelPlay.setMetaData("is_test_suite", "enable");
        LevelPlay.setMetaData("is_deviceid_optout","false");
        LevelPlay.setMetaData("is_child_directed","false");
        LevelPlayInitRequest initRequest = new LevelPlayInitRequest.Builder(_appId)
                .build();
        LevelPlayInitListener initListener = new LevelPlayInitListener() {
            @Override
            public void onInitFailed(@NonNull LevelPlayInitError error) {
                Log("OnInitFailed "+ error);
            }
            @Override
            public void onInitSuccess(LevelPlayConfiguration configuration) {
                Log("OnInitSuccess "+ configuration);

                _banner.OnReady();
                _interstitial.OnReady();
                _rewarded.OnReady();
            }
        };
        LevelPlay.addImpressionDataListener(MainActivity.this);
        LevelPlay.init(this, initRequest, initListener);

        ((ToggleButton)findViewById(R.id.demand)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isIs) {
                SetSegment(isIs);
            }
        });

        findViewById(R.id.testSuite).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LevelPlay.launchTestSuite(MainActivity.this);
            }
        });

        SetSegment(true);
    }

    @Override
    public void onImpressionSuccess(@NonNull LevelPlayImpressionData levelPlayImpressionData) {
        Log.i(_tag, "onImpressionSuccess");
    }

    void Log(String log) {
        _status.setText(log);
        Log.i(_tag, log);
    }

    private void SetSegment(boolean isIs) {
        LevelPlaySegment segment = new LevelPlaySegment();
        if (isIs) {
            segment.setSegmentName("is");
        } else {
            segment.setSegmentName("nefta");
        }
        LevelPlay.setSegment(segment);
    }
}
