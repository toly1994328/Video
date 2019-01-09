package com.toly1994.video.cameral2;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Size;
import android.view.WindowManager;

/**
 * 作者：张风捷特烈<br/>
 * 时间：2018/11/5 0005:8:39<br/>
 * 邮箱：1981462002@qq.com<br/>
 * 说明：
 */
public class Utils {
    /**
     * 获得屏幕高度
     *
     * @param ctx     上下文
     */
    public static Size loadWinSize(Context ctx) {
        WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        if (wm != null) {
            wm.getDefaultDisplay().getMetrics(outMetrics);
        }
        return new Size(outMetrics.widthPixels, outMetrics.heightPixels);
    }
}
