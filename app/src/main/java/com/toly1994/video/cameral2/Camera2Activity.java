package com.toly1994.video.cameral2;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.toly1994.video.R;
import com.toly1994.video.app.io.FileHelper;
import com.toly1994.video.app.permission.Permission;
import com.toly1994.video.app.permission.PermissionActivity;
import com.toly1994.video.app.test.L;
import com.toly1994.video.app.test.StrUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;

public class Camera2Activity extends PermissionActivity implements SurfaceHolder.Callback {
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

    private Semaphore mCameraOpenCloseLock = new Semaphore(1);//以防止在关闭相机之前应用程序退出

    private Handler mHandler = new Handler() {//Handler
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            mIdTvCountDown.setText(mCurDelayCount + "");
        }
    };

    private boolean isFlashLight;//是否开启闪光灯
    private FileOutputStream mFosVideo;
    private Handler mainHandler;
    private String mCameraID;
    private ImageReader mImageReader;
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;//相机设备
    private CameraCaptureSession mCameraCaptureSession;
    private Handler childHandler;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();//旋转方向集合

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private Size mWinSize;

    private Size justSize;
    private CameraCharacteristics mCharacteristics;
    private float mRate = 1;
    private CameraDevice.StateCallback mStateCallback;
    private VideoRecorder2Utils mVideoRecorderUtils;
    private CaptureRequest.Builder mPreviewBuilder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        applyPermissions(Permission._CAMERA, Permission._WRITE_EXTERNAL_STORAGE);


        mWinSize = Utils.loadWinSize(this);

        int rotation = getWindowManager().getDefaultDisplay().getRotation();

        mHolder = mIdSvVideo.getHolder();
        mHolder.addCallback(this);


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
            try {
                CaptureRequest.Builder reqBuilder = mCameraDevice
                        .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                reqBuilder.addTarget(mHolder.getSurface());
                reqBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
                reqBuilder.set(CaptureRequest.FLASH_MODE,
                        isFlashLight ? CameraMetadata.FLASH_MODE_TORCH : CameraMetadata.FLASH_MODE_OFF);
                mCameraCaptureSession.setRepeatingRequest(reqBuilder.build(), null, childHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        });

        //自动聚焦
        mIdSvVideo.setOnClickListener(v -> {

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


    private void takePhoto() {
        if (!isDelay) {
            takePicture();
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
                    takePicture();
                    mIdTvCountDown.setVisibility(View.GONE);
                }
            }
        });
    }

    /**
     * 录像
     */
    private void recodeVideo() {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath();
        mVideoRecorderUtils.startRecord(path, "Video",childHandler);
        mIdIvSnap.setImageTintList(ColorStateList.valueOf(0xffff0000));
    }

    /**
     * 停止录像
     */
    private void stopRecodeVideo() {
        mIdIvSnap.setImageTintList(ColorStateList.valueOf(0xff0FC2EF));
        mVideoRecorderUtils.stopRecord();

        startPreview();
    }



    /**
     * 打开指定摄像头
     */
    private void changeCamera(int id) {
        closeCamera();
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mCameraManager.openCamera(id+"", mStateCallback, childHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    /**
     * 关闭当前相机
     */
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCameraCaptureSession) {
                mCameraCaptureSession.close();
                mCameraCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }



    /**
     * 缩放封装
     */
    public void setZoom() {

        if ((mRate - 1) * 10 / 4 + 1 > 4.6f) {
            mRate = 1;
        }
        String rate = new DecimalFormat("#.0").format((mRate - 1) * 10 / 4 + 1);
        mIdIvZoom.setText(rate + "x");


        try {
            CaptureRequest.Builder reqBuilder = mCameraDevice
                    .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            // 将SurfaceView的surface作为CaptureRequest.Builder的目标
            reqBuilder.addTarget(mHolder.getSurface());
            reqBuilder.set(
                    CaptureRequest.SCALER_CROP_REGION,
                    new Rect(0, 0, (int) (justSize.getWidth() / mRate), (int) (justSize.getHeight() / mRate)));
            mCameraCaptureSession.setRepeatingRequest(reqBuilder.build(), null, childHandler);
            mRate += 0.15;
        } catch (CameraAccessException e) {
            e.printStackTrace();

        }
    }


    /**
     * 拍照方法封装
     */
    private void takePicture() {
        if (mCameraDevice != null) {
            try {
                CaptureRequest.Builder reqBuilder = mCameraDevice
                        .createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                reqBuilder.addTarget(mImageReader.getSurface());
                // 自动对焦
                reqBuilder.set(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                // 自动曝光
                reqBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                // 获取手机方向
                int rotation = getWindowManager().getDefaultDisplay().getRotation();

                // 根据设备方向计算设置照片的方向
                reqBuilder.set(
                        CaptureRequest.JPEG_ORIENTATION,
                        ORIENTATIONS.get(rotation));
                //拍照
                mCameraCaptureSession.capture(reqBuilder.build(), null, childHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void permissionOk() {


    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        HandlerThread handlerThread = new HandlerThread("Camera2");
        handlerThread.start();
        mainHandler = new Handler(getMainLooper());//主线程Handler
        childHandler = new Handler(handlerThread.getLooper());//子线程Handler


        mCameraID = "" + CameraCharacteristics.LENS_FACING_FRONT;//后摄像头
        //获取摄像头管理器
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            // 获取指定摄像头的特性
            mCharacteristics = mCameraManager.getCameraCharacteristics(mCameraID);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        Rect activeArraySize = mCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        L.d(activeArraySize + L.l());// D/Camera2Activity: Rect(0, 0 - 5184, 3880)

        Float aFloat = mCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        L.d(aFloat + L.l());//8.0


        fitPhotoSize();
        adjustScreenW(mIdSvVideo);//相机适应屏幕


        try {
            //AndroidStudio自动生成...if
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                    PackageManager.PERMISSION_GRANTED) {
                return;
            }

            mStateCallback = new CameraDevice.StateCallback() {

                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    mCameraOpenCloseLock.release();
                    mCameraDevice = camera;

                    mVideoRecorderUtils = new VideoRecorder2Utils();
                    mVideoRecorderUtils.create(mIdSvVideo, mCameraDevice,VideoRecorder2Utils.WH_720X480);
                    startPreview();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    mCameraOpenCloseLock.release();
                    mCameraDevice.close();
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    mCameraOpenCloseLock.release();
                    mCameraDevice.close();
                }
            };

            mCameraManager.openCamera(mCameraID, mStateCallback, mainHandler);


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }

    /**
     * 初始化：ImageReader
     */
    private void initImageReader() {

        mImageReader = ImageReader.newInstance(
                justSize.getWidth(), justSize.getHeight(),
                ImageFormat.JPEG, 1);

        //可以在这里处理拍照得到的临时照片
        mImageReader.setOnImageAvailableListener(reader -> {
            // 拿到拍照照片数据
            Image image = reader.acquireNextImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();

            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);//由缓冲区存入字节数组

            File file = FileHelper.get()
                    .createFile("camera2/IMG-" + StrUtil.getCurrentTime_yyyyMMddHHmmss() + ".jpg");
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
                fos.write(bytes);
                fos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fos != null) {
                        fos.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                startPreview();
                reader.close();
            }
        }, mainHandler);
    }

    /**
     * 计算该手机合适的照片尺寸
     */
    private void fitPhotoSize() {
        // 获取摄像头支持的配置属性
        StreamConfigurationMap map = mCharacteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        // 获取摄像头支持的最大尺寸
        List<Size> sizes = Arrays.asList(map.getOutputSizes(ImageFormat.JPEG));
        int minIndex = 0;//差距最小的索引
        int minDx = Integer.MAX_VALUE;
        int minDy = Integer.MAX_VALUE;
        int[] dxs = new int[sizes.size()];

        int justW = mWinSize.getHeight() * 2;//相机默认是横向的，so
        int justH = mWinSize.getWidth() * 2;

        for (int i = 0; i < sizes.size(); i++) {
            dxs[i] = sizes.get(i).getWidth() - justW;
        }

        for (int i = 0; i < dxs.length; i++) {
            int abs = Math.abs(dxs[i]);
            if (abs < minDx) {
                minIndex = i;//获取高的最适索引
                minDx = abs;
            }
        }

        for (int i = 0; i < sizes.size(); i++) {
            Size size = sizes.get(i);
            if (size.getWidth() == sizes.get(minIndex).getWidth()) {
                int dy = Math.abs(justH - size.getHeight());
                if (dy < minDy) {
                    minIndex = i;//获取宽的最适索引
                    minDy = dy;
                }
            }
        }
        justSize = sizes.get(minIndex);
    }

    /**
     * 适应屏幕
     *
     * @param surfaceView
     */
    private void adjustScreenW(View surfaceView) {
        int height = surfaceView.getHeight();
        int width = surfaceView.getWidth();
        if (height > width) {
            float justH = width * 4.f / 3;
            mIdSvVideo.setScaleX(height / justH);
        } else {
            float justW = height * 4.f / 3;
            mIdSvVideo.setScaleY(width / justW);
        }

    }



    /**
     * 开启预览
     */
    private void startPreview() {
        initImageReader();
        try {
            // 创建预览需要的CaptureRequest.Builder
            final CaptureRequest.Builder reqBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            // 将SurfaceView的surface作为CaptureRequest.Builder的目标
            reqBuilder.addTarget(mHolder.getSurface());
            //reqBuilder可以设置参数
            reqBuilder.set( // 自动对焦
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);


            reqBuilder.set(// 打开闪光灯
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

            // 创建CameraCaptureSession，该对象负责管理处理预览请求和拍照请求
            CameraCaptureSession.StateCallback stateCallback =
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            if (null == mCameraDevice) return;
                            // 当摄像头已经准备好时，开始显示预览
                            mCameraCaptureSession = cameraCaptureSession;
                            try {
                                // 显示预览
                                mCameraCaptureSession.setRepeatingRequest(
                                        reqBuilder.build(), null, childHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }

                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                            Toast.makeText(Camera2Activity.this, "配置失败", Toast.LENGTH_SHORT).show();
                        }
                    };


            mCameraDevice.createCaptureSession(
                    Arrays.asList(mHolder.getSurface(), mImageReader.getSurface()),
                    stateCallback,
                    childHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCameraDevice.close();//释放资源
    }



}
