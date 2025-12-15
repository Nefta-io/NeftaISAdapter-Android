package com.nefta.is;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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
import com.unity3d.mediation.segment.LevelPlaySegment;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements LevelPlayImpressionDataListener {
    private final String TAG = "NeftaPluginIS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        DebugServer.Init(this, getIntent());

        NeftaPlugin.EnableLogging(true);
        NeftaCustomAdapter.Init(MainActivity.this, "5658160027140096");
        NeftaPlugin.Init(getApplicationContext(), "5643649824063488").OnReady = (InitConfiguration config) -> {
            Log.i("NeftaPluginIS", "Should bypass Nefta optimization? " + config._skipOptimization);
        };

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
        Log.i(TAG, "onImpressionSuccess");
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
