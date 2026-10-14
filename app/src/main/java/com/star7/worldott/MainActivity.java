package com.star7.worldott;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

public class MainActivity extends Activity {

    private WebView webView;
    private FrameLayout rootLayout;

    private View fullScreenView;
    private WebChromeClient.CustomViewCallback fullScreenCallback;

    private ValueCallbackHolder fileChooserHolder;

    private static class ValueCallbackHolder {
        android.webkit.ValueCallback<Uri[]> callback;
    }

    private static final int FILE_CHOOSER_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(android.graphics.Color.BLACK);

        webView = new WebView(this);
        rootLayout.addView(
            webView,
            new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        );

        setContentView(rootLayout);

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient());

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onShowCustomView(
                View view,
                CustomViewCallback callback
            ) {
                if (fullScreenView != null) {
                    callback.onCustomViewHidden();
                    return;
                }

                fullScreenView = view;
                fullScreenCallback = callback;

                webView.setVisibility(View.GONE);

                rootLayout.addView(
                    fullScreenView,
                    new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                );

                getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                );

                getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                );

                setRequestedOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                );
            }

            @Override
            public void onHideCustomView() {
                exitFullScreen();
            }

            @Override
            public boolean onShowFileChooser(
                WebView webView,
                android.webkit.ValueCallback<Uri[]> filePathCallback,
                FileChooserParams fileChooserParams
            ) {
                if (fileChooserHolder != null
                        && fileChooserHolder.callback != null) {
                    fileChooserHolder.callback.onReceiveValue(null);
                }

                fileChooserHolder = new ValueCallbackHolder();
                fileChooserHolder.callback = filePathCallback;

                try {
                    Intent intent = fileChooserParams.createIntent();

                    startActivityForResult(
                        intent,
                        FILE_CHOOSER_REQUEST
                    );

                    return true;

                } catch (Exception e) {
                    fileChooserHolder.callback.onReceiveValue(null);
                    fileChooserHolder = null;
                    return false;
                }
            }
        });

        webView.loadUrl("file:///android_asset/index.html");
    }

    private void exitFullScreen() {
        if (fullScreenView == null) {
            return;
        }

        rootLayout.removeView(fullScreenView);
        fullScreenView = null;

        webView.setVisibility(View.VISIBLE);

        getWindow().clearFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_VISIBLE
        );

        setRequestedOrientation(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        );

        if (fullScreenCallback != null) {
            fullScreenCallback.onCustomViewHidden();
            fullScreenCallback = null;
        }
    }

    @Override
    protected void onActivityResult(
        int requestCode,
        int resultCode,
        Intent data
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        );

        if (requestCode == FILE_CHOOSER_REQUEST
                && fileChooserHolder != null) {

            Uri[] results = null;

            if (resultCode == RESULT_OK) {
                results =
                    WebChromeClient.FileChooserParams.parseResult(
                        resultCode,
                        data
                    );
            }

            if (fileChooserHolder.callback != null) {
                fileChooserHolder.callback.onReceiveValue(results);
            }

            fileChooserHolder = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (fullScreenView != null) {
            exitFullScreen();
        } else if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (fileChooserHolder != null
                && fileChooserHolder.callback != null) {
            fileChooserHolder.callback.onReceiveValue(null);
            fileChooserHolder = null;
        }

        if (webView != null) {
            webView.destroy();
        }

        super.onDestroy();
    }
}
