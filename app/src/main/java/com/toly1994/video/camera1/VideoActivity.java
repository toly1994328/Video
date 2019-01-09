package com.toly1994.video.camera1;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;
import android.widget.ImageView;

import com.toly1994.video.R;
import com.toly1994.video.app.permission.Permission;
import com.toly1994.video.app.permission.PermissionActivity;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 作者：张风捷特烈<br/>
 * 时间：2019/1/8 0008:16:32<br/>
 * 邮箱：1981462002@qq.com<br/>
 * 说明：
 */
public class VideoActivity extends PermissionActivity {

    private static final String TAG = "PlayActivity";
    @BindView(R.id.id_sv_video)
    SurfaceView mIdSvVideo;
    @BindView(R.id.id_iv_snap)
    ImageView mIdIvSnap;
    private VideoRecorderUtils mVideoRecorderUtils;
    private String path, name;
    private boolean isRecording;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        Log.d(TAG, "onCreate: ");
        applyPermissions(Permission._RECORD_AUDIO, Permission._CAMERA, Permission._WRITE_EXTERNAL_STORAGE);

        mVideoRecorderUtils = new VideoRecorderUtils();
        mVideoRecorderUtils.create(mIdSvVideo, VideoRecorderUtils.WH_720X480);
        path = Environment.getExternalStorageDirectory().getAbsolutePath();

        mIdIvSnap.setOnClickListener(view -> {
            if (!isRecording) {
                mVideoRecorderUtils.startRecord(path, "Video");
            } else {
                mVideoRecorderUtils.stopRecord();
            }
            isRecording = !isRecording;
        });

    }

    @Override
    protected void permissionOk() {

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: ");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: ");
        mVideoRecorderUtils.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        mVideoRecorderUtils.destroy();
    }
}
