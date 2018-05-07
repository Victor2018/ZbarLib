package zbar.victor.com.zbarlib;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.victor.zbar.interfaces.OnQrScanListener;
import com.victor.zbar.module.ZbarScanHelper;

import zbar.victor.com.library.view.AdjustTextureView;
import zbar.victor.com.library.view.ScannerFrameView;

/**
 * @author Simon Lee
 * @e-mail jmlixiaomeng@163.com
 */
public class ScanActivity extends AppCompatActivity implements OnQrScanListener {

    private AdjustTextureView mTextureView;
    private ScannerFrameView mScannerFrameView;
    private final String TAG = "ScanActivity";
    private ZbarScanHelper mZbarScanHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_relative);
        initialize();
    }

    private void initialize () {
        mTextureView = findViewById(R.id.textureview);
        mScannerFrameView = findViewById(R.id.scannerframe);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mZbarScanHelper = new ZbarScanHelper(this,mTextureView,mScannerFrameView,this);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, getClass().getName() + ".onPause()");
        super.onPause();
        mZbarScanHelper.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mZbarScanHelper.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mZbarScanHelper.onDestroy();
    }

    @Override
    public void OnQrScan(boolean scanSuccess,String resultString) {
        Toast.makeText(getApplicationContext(),resultString,Toast.LENGTH_SHORT).show();
        mZbarScanHelper.restartPreview(2000);
    }
}
