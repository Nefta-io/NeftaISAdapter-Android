package com.nefta.is;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.nefta.debug.DebugServer;
import com.nefta.sdk.InitConfiguration;
import com.nefta.sdk.NeftaPlugin;
import com.ironsource.adapters.custom.nefta.NeftaCustomAdapter;
import com.unity3d.mediation.LevelPlay;
import com.unity3d.mediation.LevelPlayConfiguration;
import com.unity3d.mediation.LevelPlayInitError;
import com.unity3d.mediation.LevelPlayInitListener;
import com.unity3d.mediation.LevelPlayInitRequest;
import com.unity3d.mediation.impression.LevelPlayImpressionData;
import com.unity3d.mediation.impression.LevelPlayImpressionDataListener;

public class MainActivity extends AppCompatActivity implements LevelPlayImpressionDataListener {
    private final String TAG = "NeftaPluginIS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        InitUI();
        DebugServer.Init(this, getIntent());

        NeftaPlugin.EnableLogging(true);
        NeftaCustomAdapter.InitWithAppId(MainActivity.this, "5657497763315712", true, (InitConfiguration config) -> {
            Log.i("NeftaPluginIS", "Nefta initialized nuid: " + config._nuid);
        });

        LevelPlay.setMetaData("is_test_suite", "enable");
        LevelPlay.setMetaData("is_deviceid_optout","false");
        LevelPlay.setMetaData("is_child_directed","false");
        LevelPlayInitRequest initRequest = new LevelPlayInitRequest.Builder(BuildConfig.IS_ID)
                .build();
        LevelPlayInitListener initListener = new LevelPlayInitListener() {
            @Override
            public void onInitFailed(@NonNull LevelPlayInitError error) {
                Log.i(TAG, "OnInitFailed "+ error);
            }
            @Override
            public void onInitSuccess(LevelPlayConfiguration configuration) {
                Log.i(TAG, "OnInitSuccess "+ configuration);
            }
        };
        LevelPlay.addImpressionDataListener(this);
        LevelPlay.init(this, initRequest, initListener);

        findViewById(R.id.testSuite).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LevelPlay.launchTestSuite(MainActivity.this);
            }
        });
    }

    @Override
    public void onImpressionSuccess(@NonNull LevelPlayImpressionData levelPlayImpressionData) {
        Log.i(TAG, "onImpressionSuccess");
    }

    private void InitUI() {
        TextView title = findViewById(R.id.title);
        title.setText("LevelPlay Integration "+ LevelPlay.getSdkVersion());

        findViewById(R.id.control).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.groupView).setVisibility(View.GONE);

                ((InterstitialUi)findViewById(R.id.interstitial)).Init(new InterstitialDefault());
                ((RewardedUi)findViewById(R.id.rewarded)).Init(new RewardedDefault());
            }
        });
        findViewById(R.id.optimized).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.groupView).setVisibility(View.GONE);

                ((InterstitialUi)findViewById(R.id.interstitial)).Init(new InterstitialOptimized());
                ((RewardedUi)findViewById(R.id.rewarded)).Init(new RewardedOptimized());
            }
        });
        findViewById(R.id.simulator).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.groupView).setVisibility(View.GONE);

                findViewById(R.id.interstitialSim).setVisibility(View.VISIBLE);
                findViewById(R.id.rewardedSim).setVisibility(View.VISIBLE);
            }
        });
    }
}
