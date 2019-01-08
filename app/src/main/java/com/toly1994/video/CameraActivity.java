package com.toly1994.video;

import android.content.res.ColorStateList;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.toly1994.video.app.io.FileHelper;
import com.toly1994.video.app.permission.Permission;
import com.toly1994.video.app.permission.PermissionActivity;
import com.toly1994.video.app.test.L;
import com.toly1994.video.app.test.ToastUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CameraActivity extends PermissionActivity implements SurfaceHolder.Callback {
    private static final String TAG = "CameraActivity";
    @BindView(R.id.id_sv_video)
    SurfaceView mIdSvVideo;
    @BindView(R.id.id_iv_snap)
    ImageView mIdIvSnap;
    @BindView(R.id.id_tv_count_down)
    TextView mIdTvCountDown;
    @BindView(R.id.id_iv_delay)
    ImageView mIdIvDelay;
    @BindView(R.id.id_iv_zoom)
    TextView mIdIvZoom;
    @BindView(R.id.id_iv_splash)
    ImageView mIdIvSplash;
    @BindView(R.id.id_iv_switch)
    ImageView mIdIvSwitch;
    @BindView(R.id.id_tv_pic)
    TextView mIdTvPic;
    @BindView(R.id.id_tv_video)
    TextView mIdTvVideo;
    private Camera camera;
    private SurfaceHolder mHolder;

    private boolean isDelay = false;//是否延迟

    private static final int DEFAULT_DELAY_COUNT = 3 + 1;//默认延迟时间3s
    private int mCurDelayCount = DEFAULT_DELAY_COUNT;//当前倒计时时间

    private int currZoom;//当前缩放数


    private static final int FRONT = 1;//前置摄像头标记
    private static final int BACK = 0;//后置摄像头标记
    private boolean isBack;//是否是前置

    private boolean isPhoto = true;//是否是拍照
    private boolean isRecoding;//是否在录像
    private int clickRecordCount = 0;//录屏时的点击录屏次数

    private Handler mHandler = new Handler() {//Handler
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            mIdTvCountDown.setText(mCurDelayCount + "");
        }
    };
    private Camera.Parameters mParameters;
    private boolean isFlashLight;//是否开启闪光灯
    private FileOutputStream mFosVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        applyPermissions(Permission._CAMERA, Permission._WRITE_EXTERNAL_STORAGE);

        mHolder = mIdSvVideo.getHolder();
        mHolder.addCallback(this);


        camera = Camera.open(BACK);// 打开摄像头
        camera.setDisplayOrientation(90);//并将展示方向旋转90度--水平-->竖直

        mParameters = camera.getParameters();
        mParameters.setPreviewFormat(ImageFormat.NV21);
//        mParameters.setPreviewFpsRange();

        mParameters.setPictureSize(720, 480);//设置图片尺寸
        mParameters.setPreviewSize(720, 480);//设置预览尺寸
//        mParameters.setPreviewSize(1080 / 2, 2340 / 2);


        camera.setParameters(mParameters);

        List<Camera.Size> pictureSizes = camera.getParameters().getSupportedPictureSizes();
        List<Camera.Size> previewSizes = camera.getParameters().getSupportedPreviewSizes();

        for (int i = 0; i < pictureSizes.size(); i++) {
            Camera.Size pSize = pictureSizes.get(i);
            L.d("PictureSize.width = " + pSize.width + "--------PictureSize.height = " + pSize.height);
        }

        for (int i = 0; i < previewSizes.size(); i++) {
            Camera.Size pSize = previewSizes.get(i);
            L.d("previewSize.width = " + pSize.width + "-------previewSize.height = " + pSize.height);
        }


        L.d("onCreate" + L.l());


        //视频录像
        camera.setPreviewCallback((data, camera) -> {
                    if (isRecoding) {
                        L.d("onPreviewFrame--" + Thread.currentThread().getName() + L.l());
                        collectData(data);
                    }
                }
        );


        mIdIvSnap.setOnClickListener(v -> {
            if (isPhoto) {
                takePhoto();//照相
            } else {
                if (clickRecordCount % 2 == 0) {
                    recodeVideo();//录制
                } else {
                    stopRecodeVideo();//停止录制
                }
            }
            clickRecordCount++;
        });

        //是否倒计时
        mIdIvDelay.setOnClickListener(v -> {
            if (!isDelay) {
                mIdIvDelay.setImageTintList(ColorStateList.valueOf(0xffEFB90F));
            } else {
                mIdIvDelay.setImageTintList(ColorStateList.valueOf(0xfffffffF));
            }
            isDelay = !isDelay;
        });

        //开闪光灯
        mIdIvSplash.setOnClickListener(v -> {
            if (!isFlashLight) {
                mIdIvSplash.setImageTintList(ColorStateList.valueOf(0xffEFB90F));
            } else {
                mIdIvSplash.setImageTintList(ColorStateList.valueOf(0xfffffffF));
            }
            isFlashLight = !isFlashLight;
            mParameters.setFlashMode(
                    isFlashLight ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
            camera.setParameters(mParameters);
        });

        //自动聚焦
        mIdSvVideo.setOnClickListener(v -> {
            camera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                }
            });
        });

        //缩放变焦
        mIdIvZoom.setOnClickListener(v -> {
            setZoom();
        });

        //切换镜头
        mIdIvSwitch.setOnClickListener(v -> {
            if (!isBack) {
                mIdIvSwitch.setImageTintList(ColorStateList.valueOf(0xffEFB90F));
            } else {
                mIdIvSwitch.setImageTintList(ColorStateList.valueOf(0xfffffffF));
            }
            changeCamera(isBack ? BACK : FRONT);
            isBack = !isBack;
        });

        //切换录制
        mIdTvVideo.setOnClickListener(v -> {
            mIdTvVideo.setTextColor(0xffEFB90F);
            mIdTvPic.setTextColor(0xfffffffF);
            mIdIvSnap.setImageTintList(ColorStateList.valueOf(0xff0FC2EF));
            isPhoto = false;
        });

        //切换录制
        mIdTvPic.setOnClickListener(v -> {
            mIdTvVideo.setTextColor(0xfffffffF);
            mIdIvSnap.setImageTintList(ColorStateList.valueOf(0x88ffffff));
            mIdTvPic.setTextColor(0xffEFB90F);
            isPhoto = true;
        });
    }

    /**
     * 收集数据
     *
     * @param data
     */
    private void collectData(byte[] data) {
        try {
            mFosVideo.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void takePhoto() {
        if (!isDelay) {
            takePicture("pic/hello.jpg");
            return;
        }
        mIdTvCountDown.setVisibility(View.VISIBLE);
        mCurDelayCount = DEFAULT_DELAY_COUNT;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mCurDelayCount > 0) {
                    mCurDelayCount--;
                    L.d(mCurDelayCount + L.l());
                    mHandler.postDelayed(this, 1000);
                    mHandler.sendEmptyMessage(0);
                } else {
                    takePicture("pic/hello.jpg");
                    mIdTvCountDown.setVisibility(View.GONE);
                }
            }
        });
    }

    /**
     * 录像
     */
    private void recodeVideo() {
        isRecoding = true;
        File videoFile = FileHelper.get().createFile("video/hello");

        try {
            mFosVideo = new FileOutputStream(videoFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        mIdIvSnap.setImageTintList(ColorStateList.valueOf(0xffff0000));
        camera.startPreview();
    }

    /**
     * 停止录像
     */
    private void stopRecodeVideo() {
        isRecoding = false;
        mIdIvSnap.setImageTintList(ColorStateList.valueOf(0xff0FC2EF));
        try {
            mFosVideo.flush();
            mFosVideo.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 切换相机
     *
     * @param type
     */
    private void changeCamera(int type) {
        camera.stopPreview();
        camera.release();
        camera = openCamera(type);
        try {
            camera.setPreviewDisplay(mHolder);
            camera.setDisplayOrientation(90);//并将展示方向旋转90度--水平-->竖直
        } catch (IOException e) {
            e.printStackTrace();
        }
        camera.startPreview();
    }

    private Camera openCamera(int type) {
        int frontIndex = -1;
        int backIndex = -1;
        int cameraCount = Camera.getNumberOfCameras();
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int cameraIndex = 0; cameraIndex < cameraCount; cameraIndex++) {
            Camera.getCameraInfo(cameraIndex, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                frontIndex = cameraIndex;
            } else if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                backIndex = cameraIndex;
            }
        }
        if (type == FRONT && frontIndex != -1) {
            return Camera.open(frontIndex);
        } else if (type == BACK && backIndex != -1) {
            return Camera.open(backIndex);
        }
        return null;
    }

    /**
     * 缩放封装
     */
    public void setZoom() {
        if (mParameters.isZoomSupported()) {//是否支持缩放
            try {
                Camera.Parameters params = mParameters;
                final int maxZoom = params.getMaxZoom();
                if (maxZoom == 0) return;
                currZoom = params.getZoom();
                currZoom += maxZoom / 10;
                if (currZoom > maxZoom) {
                    currZoom = 0;
                }
                params.setZoom(currZoom);
                camera.setParameters(params);
                String rate = new DecimalFormat("#.0").format(currZoom / (maxZoom / 10 * 2.f) + 1);
                mIdIvZoom.setText(rate + "x");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            ToastUtil.show(this, "您的手机不支持变焦功能!");
        }
    }


    /**
     * 拍照方法封装
     *
     * @param name 图片名称(加文件夹：形式如：pic/hello.jpg)
     */
    private void takePicture(String name) {
        camera.takePicture(null, null, (data, camera) -> {
            File pic = FileHelper.get().createFile(name);
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(pic);
                fos.write(data);
                fos.flush();
                fos.close();
                camera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    assert fos != null;
                    fos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
    }


    @Override
    protected void permissionOk() {


    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            camera.setPreviewDisplay(holder);//Camera+SurfaceHolder
            camera.startPreview();//开启预览
        } catch (IOException e) {
            e.printStackTrace();
            camera.release();//释放资源
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        camera.release();//释放资源
    }
}
