package com.toly1994.video.camera1;

import android.graphics.Point;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.File;
import java.io.IOException;

/**
 * 作者：张风捷特烈<br/>
 * 时间：2019/1/8 0008:16:29<br/>
 * 邮箱：1981462002@qq.com<br/>
 * 说明：视频录制辅助类
 */
public class VideoRecorderUtils {
    private MediaRecorder mediaRecorder;
    private Camera camera;
    private SurfaceHolder.Callback callback;
    private SurfaceView surfaceView;
    private int height, width;

    public static Point WH_2160X1080 = new Point(2160, 1080);
    public static Point WH_1920X1080 = new Point(1920, 1080);
    public static Point WH_1280X960 = new Point(1280, 960);
    public static Point WH_1440X720 = new Point(1440, 720);
    public static Point WH_1280X720 = new Point(1280, 720);
    public static Point WH_864X480 = new Point(864, 480);
    public static Point WH_800X480 = new Point(800, 480);
    public static Point WH_720X480 = new Point(720, 480);
    public static Point WH_640X480 = new Point(640, 480);
    public static Point WH_352X288 = new Point(352, 288);
    public static Point WH_320X240 = new Point(320, 240);
    public static Point WH_176X144 = new Point(176, 144);


    public void create(SurfaceView surfaceView,Point point) {
        this.surfaceView = surfaceView;
        surfaceView.setKeepScreenOn(true);
        callback = new SurfaceHolder.Callback() {
            public void surfaceCreated(SurfaceHolder holder) {
                camera = Camera.open();
                width = point.x;
                height = point.y;
                mediaRecorder = new MediaRecorder();
            }

            public void surfaceChanged(SurfaceHolder holder, int format,
                                       int width, int height) {
                doChange(holder);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (camera != null) {
                    camera.release();
                    camera = null;
                }
            }
        };
        surfaceView.getHolder().addCallback(callback);
    }

    private void doChange(SurfaceHolder holder) {
        try {
            camera.setPreviewDisplay(holder);
            camera.setDisplayOrientation(90);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void stopRecord() {
        mediaRecorder.release();
        camera.release();
        mediaRecorder = null;
        camera = Camera.open();
        mediaRecorder = new MediaRecorder();
        doChange(surfaceView.getHolder());
    }


    public void stop() {
        if (mediaRecorder != null && camera != null) {
            mediaRecorder.release();
            camera.release();
        }
    }

    public void destroy() {
        if (mediaRecorder != null && camera != null) {
            mediaRecorder.release();
            camera.release();
            mediaRecorder = null;
            camera = null;
        }
    }

    /**
     * @param path 保存的路径
     * @param name 录像视频名称(不包含后缀)
     */
    public void startRecord(String path, String name) {
        camera.unlock();
        mediaRecorder.setCamera(camera);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
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
        mediaRecorder.setPreviewDisplay(surfaceView.getHolder().getSurface());
        mediaRecorder.setOrientationHint(0);
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
