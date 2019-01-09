package com.toly1994.video.cameral2;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 作者：张风捷特烈<br/>
 * 时间：2019/1/8 0008:16:29<br/>
 * 邮箱：1981462002@qq.com<br/>
 * 说明：视频录制辅助类
 */
public class VideoRecorder2Utils {
    private MediaRecorder mediaRecorder;
    private SurfaceHolder.Callback callback;
    private SurfaceView surfaceView;
    private CameraDevice mCameraDevice;
    private int height;
    private int width;
    List<Surface> surfaces = new ArrayList<>();

    public static Size WH_2160X1080 = new Size(2160, 1080);
    public static Size WH_1920X1080 = new Size(1920, 1080);
    public static Size WH_1280X960 = new Size(1280, 960);
    public static Size WH_1440X720 = new Size(1440, 720);
    public static Size WH_1280X720 = new Size(1280, 720);
    public static Size WH_864X480 = new Size(864, 480);
    public static Size WH_800X480 = new Size(800, 480);
    public static Size WH_720X480 = new Size(720, 480);
    public static Size WH_640X480 = new Size(640, 480);
    public static Size WH_352X288 = new Size(352, 288);
    public static Size WH_320X240 = new Size(320, 240);
    public static Size WH_176X144 = new Size(176, 144);
    private CaptureRequest.Builder mPreviewBuilder;
    private CaptureRequest mCaptureRequest;
    private CameraCaptureSession mPreviewSession;


    public void create(SurfaceView surfaceView, CameraDevice cameraDevice, Size size) {
        this.surfaceView = surfaceView;
        mCameraDevice = cameraDevice;

        //创建录制的session会话中的请求
        try {
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        height = size.getHeight();
        width = size.getWidth();
        mediaRecorder = new MediaRecorder();
    }

    public void stopRecord() {
        mediaRecorder.release();
        mediaRecorder = null;
        mediaRecorder = new MediaRecorder();
        surfaces.clear();

    }


    public void stop() {
        if (mediaRecorder != null) {
            mediaRecorder.release();
        }
    }

    public void destroy() {
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    /**
     * @param path 保存的路径
     * @param name 录像视频名称(不包含后缀)
     */
    public void startRecord(String path, String name, Handler handler) {
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setVideoEncodingBitRate(700 * 1024);
        mediaRecorder.setVideoSize(width, height);
        mediaRecorder.setVideoFrameRate(24);
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
        mediaRecorder.setOutputFile(path + File.separator + name + ".mp4");
        File file1 = new File(path + File.separator + name + ".mp4");
        if (file1.exists()) {
            file1.delete();
        }
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        review(handler);
    }

    public void review(Handler handler) {
        Surface previewSurface = surfaceView.getHolder().getSurface();
        surfaces.add(previewSurface);
        mPreviewBuilder.addTarget(previewSurface);

        Surface recorderSurface = mediaRecorder.getSurface();
        surfaces.add(recorderSurface);
        mPreviewBuilder.addTarget(recorderSurface);

        try {
            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        //创建捕获请求
                        mCaptureRequest = mPreviewBuilder.build();
                        mPreviewSession = session;
                        //设置反复捕获数据的请求，这样预览界面就会一直有数据显示
                        mPreviewSession.setRepeatingRequest(mCaptureRequest, null, handler);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //清除预览Session
    private void closePreviewSession() {
        if (mPreviewSession != null) {
            mPreviewSession.close();
            mPreviewSession = null;
        }
    }
}
