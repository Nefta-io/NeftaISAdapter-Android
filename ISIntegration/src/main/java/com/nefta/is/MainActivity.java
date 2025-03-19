package com.nefta.is;

import android.app.BackgroundServiceStartNotAllowedException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.multidex.BuildConfig;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.IronSourceSegment;
import com.ironsource.mediationsdk.WaterfallConfiguration;
import com.ironsource.mediationsdk.impressionData.ImpressionData;
import com.ironsource.mediationsdk.impressionData.ImpressionDataListener;
import com.ironsource.mediationsdk.integration.IntegrationHelper;
import com.ironsource.mediationsdk.sdk.SegmentListener;
import com.nefta.sdk.Insight;
import com.nefta.sdk.NeftaPlugin;
import com.ironsource.adapters.custom.nefta.NeftaCustomAdapter;
import com.unity3d.mediation.LevelPlay;
import com.unity3d.mediation.LevelPlayConfiguration;
import com.unity3d.mediation.LevelPlayInitError;
import com.unity3d.mediation.LevelPlayInitListener;
import com.unity3d.mediation.LevelPlayInitRequest;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements SegmentListener, ImpressionDataListener {

    private final String _tag = "IronSourceIntegration";
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

        NeftaPlugin plugin = NeftaCustomAdapter.Init(MainActivity.this, "5643649824063488");
        plugin.GetBehaviourInsight(new String[] {
                "p_churn_14d", "pred_total_value", "pred_ecpm_banner"
        });
        plugin.OnBehaviourInsight = this::OnBehaviourInsight;

        _status = findViewById(R.id.status);

        _banner = new BannerWrapper(this, findViewById(R.id.bannerView), findViewById(R.id.showBanner), findViewById(R.id.closeBanner));
        _interstitial = new InterstitialWrapper(this, findViewById(R.id.loadInterstitial), findViewById(R.id.showInterstitial));
        _rewarded = new RewardedWrapper(this, findViewById(R.id.loadRewardedVideo), findViewById(R.id.showRewardedVideo));

        IronSource.setMetaData("is_test_suite", "enable");
        IronSource.setMetaData("is_deviceid_optout","false");
        IronSource.setMetaData("is_child_directed","false");
        LevelPlayInitRequest initRequest = new LevelPlayInitRequest.Builder(_appId)
                .withUserId("user123")
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
        IronSource.setSegmentListener(MainActivity.this);
        IronSource.addImpressionDataListener(MainActivity.this);
        LevelPlay.init(this, initRequest, initListener);

        if (BuildConfig.DEBUG){
            IntegrationHelper.validateIntegration(this);
        }

        ((ToggleButton)findViewById(R.id.demand)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isIs) {
                SetSegment(isIs);
            }
        });

        findViewById(R.id.testSuite).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IronSource.launchTestSuite(MainActivity.this);
            }
        });

        SetSegment(false);
    }

    private void OnBehaviourInsight(HashMap<String, Insight> insights) {
        for (Map.Entry<String, Insight> insight : insights.entrySet()) {
            Insight insightValue = insight.getValue();
            Log.i(_tag, "Insight "+ insight.getKey() + " s:"+ insightValue._status + ", i:"+ insightValue._int + " f:"+ insightValue._float + " s:"+ insightValue._string);
        }

        double pred_total_value = insights.get("pred_total_value")._float;
        double pred_ecpm_banner = insights.get("pred_ecpm_banner")._float;
        double user_value_spread = pred_total_value - pred_ecpm_banner;

        if (user_value_spread > 0) {
            double bid_floor_price = pred_ecpm_banner + user_value_spread;

            WaterfallConfiguration configuration = WaterfallConfiguration.builder().setCeiling(40).setFloor(30).build();
            IronSource.setWaterfallConfiguration(configuration, IronSource.AD_UNIT.BANNER);
            Log.i(_tag, "Insight price: "+ bid_floor_price);
        }
    }

    public void onSegmentReceived(String segment) {
        Log.i(_tag, "onSegmentReceived: "+ segment);
    }

    public void onImpressionSuccess(ImpressionData impression) {
        Log.i(_tag, "onImpressionSuccess");
    }

    @Override
    protected void onResume() {
        super.onResume();
        IronSource.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        IronSource.onPause(this);
    }

    void Log(String log) {
        _status.setText(log);
        Log.i(_tag, log);
    }

    private void SetSegment(boolean isIs) {
        IronSourceSegment segment = new IronSourceSegment();
        if (isIs) {
            segment.setSegmentName("is");
        } else {
            segment.setSegmentName("nefta");
        }
        IronSource.setSegment(segment);
    }
}
