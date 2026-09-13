package com.github.tvbox.osc.base;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import androidx.core.os.HandlerCompat;
import androidx.multidex.MultiDexApplication;

import com.github.catvod.crawler.JarLoader;
import com.github.catvod.crawler.JsLoader;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.callback.EmptyCallback;
import com.github.tvbox.osc.callback.LoadingCallback;
import com.github.tvbox.osc.data.AppDataManager;
import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.util.EpgUtil;
import com.github.tvbox.osc.util.FileUtils;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.LocaleHelper;
import com.github.tvbox.osc.util.LOG;
import com.github.tvbox.osc.util.OkGoHelper;
import com.github.tvbox.osc.util.PlayerHelper;
import com.github.tvbox.osc.util.SubtitleHelper;
import com.hjq.permissions.XXPermissions;
import com.kingja.loadsir.core.LoadSir;
import com.orhanobut.hawk.Hawk;
import com.p2p.P2PClass;
import com.whl.quickjs.android.QuickJSLoader;
import com.yanzhenjie.andserver.AndServer;
import com.yanzhenjie.andserver.Server;

import java.io.File;
import java.util.concurrent.TimeUnit;

import io.github.inflationx.calligraphy3.CalligraphyConfig;
import io.github.inflationx.calligraphy3.CalligraphyInterceptor;
import io.github.inflationx.viewpump.ViewPump;
import me.jessyan.autosize.AutoSizeConfig;
import me.jessyan.autosize.unit.Subunits;

/**
 * @author pj567
 * @date :2020/12/17
 * @description:
 */
public class App extends MultiDexApplication {
    private static App instance;
    private static P2PClass p;
    public static String burl;
    private static String dashData;
    public static ViewPump viewPump = null;
    private static Server server = null;
    private final Handler handler;

    public App() {
        instance = this;
        handler = HandlerCompat.createAsync(Looper.getMainLooper());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 全能影库: 首次启动预设备用配置源（设置→配置地址→历史记录可一键切换）
        if (com.orhanobut.hawk.Hawk.get(com.github.tvbox.osc.util.HawkConfig.API_HISTORY, new java.util.ArrayList<String>()).isEmpty()) {
            java.util.ArrayList<String> seeds = new java.util.ArrayList<>();
            seeds.add("http://www.xn--sss604efuw.net/tv");
            seeds.add("https://tv.nxog.top/moyu.json");
            seeds.add("https://chuanshuo.77blog.cn/tv.json");
            com.orhanobut.hawk.Hawk.put(com.github.tvbox.osc.util.HawkConfig.API_HISTORY, seeds);
        }
        SubtitleHelper.initSubtitleColor(this);
        initParams();
        // takagen99 : Initialize Locale
        initLocale();
        // OKGo
        OkGoHelper.init();
        // 闂叧妫€鏌ユā寮?        XXPermissions.setCheckMode(false);
        // Get EPG Info
        EpgUtil.init();
        // 鍒濆鍖朩eb鏈嶅姟鍣?        ControlManager.init(this);
        //鍒濆鍖栨暟鎹簱
        AppDataManager.init();
        LoadSir.beginBuilder()
                .addCallback(new EmptyCallback())
                .addCallback(new LoadingCallback())
                .commit();
        AutoSizeConfig.getInstance().setCustomFragment(true).getUnitsManager()
                .setSupportDP(false)
                .setSupportSP(false)
                .setSupportSubunits(Subunits.MM);
        // 淇 Android 14+锛氬喎鍚姩鏃?ScreenUtils 鍙兘鑾峰彇鍒扮珫灞忓搴︼紝瀵艰嚧 xdpi 璁＄畻閿欒銆乁I 鍙樺皬
        int screenWidth = AutoSizeConfig.getInstance().getScreenWidth();
        int screenHeight = AutoSizeConfig.getInstance().getScreenHeight();
        if (screenWidth < screenHeight) {
            AutoSizeConfig.getInstance().setScreenWidth(screenHeight);
            AutoSizeConfig.getInstance().setScreenHeight(screenWidth);
        }
        PlayerHelper.init();

        // Delete Cache
        /*File dir = getCacheDir();
        FileUtils.recursiveDelete(dir);
        dir = getExternalCacheDir();
        FileUtils.recursiveDelete(dir);*/

        FileUtils.cleanPlayerCache();

        // Add JS support
        QuickJSLoader.init();

        // add font support, my tv embed font not include emoji
        String extStorageDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        File fontFile = new File(extStorageDir + "/tvbox.ttf");
        if (fontFile.exists()) {
            viewPump = ViewPump.builder()
                    .addInterceptor(new CalligraphyInterceptor(
                            new CalligraphyConfig.Builder()
                                    .setDefaultFontPath(fontFile.getAbsolutePath())
                                    .setFontAttrId(R.attr.fontPath)
                                    .build()))
                    .build();
        }
    }

    public static P2PClass getp2p() {
        try {
            if (p == null) {
                p = new P2PClass(FileUtils.getExternalCachePath());
            }
            return p;
        } catch (Exception e) {
            LOG.e(e.toString());
            return null;
        }
    }


    private void initParams() {
        // Hawk
        Hawk.init(this).build();
        Hawk.put(HawkConfig.DEBUG_OPEN, false);

        // 棣栭〉閫夐」
        putDefault(HawkConfig.HOME_SHOW_SOURCE, true);       //鏁版嵁婧愭樉绀? true=寮€鍚? false=鍏抽棴
        putDefault(HawkConfig.HOME_SEARCH_POSITION, false);  //鎸夐挳浣嶇疆-鎼滅储: true=涓婃柟, false=涓嬫柟
        putDefault(HawkConfig.HOME_MENU_POSITION, true);     //鎸夐挳浣嶇疆-璁剧疆: true=涓婃柟, false=涓嬫柟
        putDefault(HawkConfig.HOME_REC, 1);                  //鎺ㄨ崘: 0=璞嗙摚鐑挱, 1=绔欑偣鎺ㄨ崘, 2=瑙傜湅鍘嗗彶
        putDefault(HawkConfig.HOME_NUM, 4);                  //鍘嗗彶鏉℃暟: 0=20鏉? 1=40鏉? 2=60鏉? 3=80鏉? 4=100鏉?        // 鎾斁鍣ㄩ€夐」
        putDefault(HawkConfig.SHOW_PREVIEW, true);           //绐楀彛棰勮: true=寮€鍚? false=鍏抽棴
        putDefault(HawkConfig.PLAY_SCALE, 0);                //鐢婚潰缂╂斁: 0=榛樿, 1=16:9, 2=4:3, 3=濉厖, 4=鍘熷, 5=瑁佸壀
        putDefault(HawkConfig.BACKGROUND_PLAY_TYPE, 0);      //鍚庡彴锛?=鍏抽棴, 1=寮€鍚? 2=鐢讳腑鐢?        putDefault(HawkConfig.PLAY_TYPE, 1);                 //鎾斁鍣? 0=绯荤粺, 1=IJK, 2=Exo, 3=MX, 4=Reex, 5=Kodi
        putDefault(HawkConfig.IJK_CODEC, "纭В鐮?);           //IJK瑙ｇ爜: 杞В鐮? 纭В鐮?        // 绯荤粺閫夐」
        putDefault(HawkConfig.HOME_LOCALE, 0);               //璇█: 0=涓枃, 1=鑻辨枃
        putDefault(HawkConfig.THEME_SELECT, 0);              //涓婚: 0=濂堥, 1=鍝嗗暒, 2=鐧句簨, 3=楦ｄ汉, 4=灏忛粍, 5=鍏, 6=妯辫姳
        putDefault(HawkConfig.SEARCH_VIEW, 1);               //鎼滅储灞曠ず: 0=鏂囧瓧鍒楄〃, 1=缂╃暐鍥?        putDefault(HawkConfig.PARSE_WEBVIEW, true);          //鍡呮帰Webview: true=绯荤粺鑷甫, false=XWalkView
        putDefault(HawkConfig.DOH_URL, 0);                   //瀹夊叏DNS: 0=鍏抽棴, 1=鑵捐, 2=闃块噷, 3=360, 4=Google, 5=AdGuard, 6=Quad9

    }

    private void initLocale() {
        if (Hawk.get(HawkConfig.HOME_LOCALE, 0) == 0) {
            LocaleHelper.setLocale(App.this, "zh");
        } else {
            LocaleHelper.setLocale(App.this, "");
        }
    }

    public static App getInstance() {
        return instance;
    }

    private void putDefault(String key, Object value) {
        if (!Hawk.contains(key)) {
            Hawk.put(key, value);
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        JsLoader.destroy();
    }

    public void setDashData(String data) {
        dashData = data;
    }

    public String getDashData() {
        return dashData;
    }

    public static void startWebserver() {
        if (server != null) return;
        server = AndServer
                .webServer(instance)
                .port(12345)
                .timeout(60, TimeUnit.SECONDS)
                .listener(new Server.ServerListener() {
                    @Override
                    public void onStarted() {

                    }

                    @Override
                    public void onStopped() {

                    }

                    @Override
                    public void onException(Exception e) {

                    }
                }).build();
        server.startup();
    }

    public static void post(Runnable runnable) {
        getInstance().handler.post(runnable);
    }

    public static void post(Runnable runnable, long delayMillis) {
        getInstance().handler.removeCallbacks(runnable);
        if (delayMillis >= 0) getInstance().handler.postDelayed(runnable, delayMillis);
    }
}
