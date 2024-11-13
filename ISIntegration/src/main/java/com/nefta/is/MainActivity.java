package com.nefta.is;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.multidex.BuildConfig;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.integration.IntegrationHelper;
import com.ironsource.mediationsdk.sdk.InitializationListener;
import com.nefta.sdk.NeftaPlugin;

public class MainActivity extends AppCompatActivity implements InitializationListener {

    private final String _appId = "1bb635bc5";

    private TextView _status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        NeftaPlugin.Init(this, "5643649824063488");

        _status = findViewById(R.id.status);

        IronSource.setUserId("test123");

        new BannerWrapper(this, findViewById(R.id.bannerView), findViewById(R.id.showBanner), findViewById(R.id.closeBanner));
        new InterstitialWrapper(this, findViewById(R.id.loadInterstitial), findViewById(R.id.showInterstitial));
        new RewardedWrapper(this, findViewById(R.id.loadRewardedVideo), findViewById(R.id.showRewardedVideo));

        IronSource.init(this, _appId, IronSource.AD_UNIT.BANNER, IronSource.AD_UNIT.INTERSTITIAL, IronSource.AD_UNIT.REWARDED_VIDEO);

        if (BuildConfig.DEBUG){
            IntegrationHelper.validateIntegration(this);
        }
    }

    @Override
    public void onInitializationComplete() {
        Log("onInitializationComplete");
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
        Log.i("IronSourceIntegration", log);
    }
}
