package com.star7.worldott;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class MainActivity extends Activity {
    private WebView web;
    private FrameLayout root;
    private View fullscreen;
    private WebChromeClient.CustomViewCallback fullscreenCallback;
    private ValueCallback<Uri[]> fileCallback;
    private static final int PICK_FILE = 7001;
    private boolean videoPlaying = false;

    // Only expose this bridge to bundled trusted HTML, never arbitrary websites.
    public class VideoBridge {
        @JavascriptInterface public void setVideoPlaying(boolean playing) {
            runOnUiThread(() -> { videoPlaying = playing; updateKeepAwake(); });
        }
    }
    private void updateKeepAwake() {
        if (videoPlaying) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        // Do not draw content behind Android status/navigation bars.
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        // Draw dark behind the system clock and three-button navigation area.
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.rgb(8,9,14)));
        getWindow().getDecorView().setBackgroundColor(Color.rgb(8,9,14));
        getWindow().setStatusBarColor(Color.rgb(8,9,14));
        getWindow().setNavigationBarColor(Color.rgb(8,9,14));
        if (Build.VERSION.SDK_INT >= 29) {
            getWindow().setNavigationBarContrastEnforced(false);
            getWindow().setStatusBarContrastEnforced(false);
        }
        getWindow().getDecorView().setSystemUiVisibility(0); // light icons on dark background
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(8,9,14));
        root.setFitsSystemWindows(true); // keep tappable content away from bars
        web = new WebView(this);
        web.setBackgroundColor(Color.rgb(8,9,14));
        root.addView(web,new FrameLayout.LayoutParams(-1,-1));
        setContentView(root);
        WebSettings s=web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        web.addJavascriptInterface(new VideoBridge(),"Star7Native");
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onShowCustomView(View view,CustomViewCallback cb) {
                if(fullscreen!=null){cb.onCustomViewHidden();return;}
                fullscreen=view;fullscreenCallback=cb;
                web.setVisibility(View.GONE);
                root.addView(view,new FrameLayout.LayoutParams(-1,-1));
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
            @Override public void onHideCustomView(){closeFullscreen();}
            @Override public boolean onShowFileChooser(WebView view,ValueCallback<Uri[]> cb,FileChooserParams params){
                if(fileCallback!=null)fileCallback.onReceiveValue(null);
                fileCallback=cb;
                try{startActivityForResult(params.createIntent(),PICK_FILE);return true;}
                catch(Exception e){fileCallback.onReceiveValue(null);fileCallback=null;return false;}
            }
        });
        web.loadUrl("file:///android_asset/index.html");
    }
    private void closeFullscreen(){
        if(fullscreen==null)return;
        root.removeView(fullscreen);fullscreen=null;web.setVisibility(View.VISIBLE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        if(fullscreenCallback!=null){fullscreenCallback.onCustomViewHidden();fullscreenCallback=null;}
        updateKeepAwake();
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==PICK_FILE&&fileCallback!=null){fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result,data));fileCallback=null;}
    }
    @Override public void onBackPressed(){if(fullscreen!=null)closeFullscreen();else if(web!=null&&web.canGoBack())web.goBack();else super.onBackPressed();}
    @Override protected void onDestroy(){videoPlaying=false;updateKeepAwake();if(web!=null){web.destroy();web=null;}super.onDestroy();}
}
