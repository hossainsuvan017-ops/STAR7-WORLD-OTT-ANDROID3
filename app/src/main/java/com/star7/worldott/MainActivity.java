package com.star7.worldott;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.webkit.ValueCallback;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

public class MainActivity extends Activity {

    private WebView web;
    private ValueCallback<Uri[]> fileCallback;

    private static final int PICK_FILE = 7001;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        web = new WebView(this);

        web.setLayoutParams(
            new FrameLayout.LayoutParams(-1, -1)
        );

        setContentView(web);

        WebSettings s = web.getSettings();

        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);

        web.setWebViewClient(new WebViewClient());

        web.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onShowFileChooser(
                WebView view,
                ValueCallback<Uri[]> callback,
                FileChooserParams params
            ) {

                if (fileCallback != null) {
                    fileCallback.onReceiveValue(null);
                }

                fileCallback = callback;

                try {
                    startActivityForResult(
                        params.createIntent(),
                        PICK_FILE
                    );

                    return true;

                } catch (Exception e) {

                    fileCallback = null;

                    Toast.makeText(
                        MainActivity.this,
                        "File picker unavailable",
                        Toast.LENGTH_SHORT
                    ).show();

                    return false;
                }
            }
        });

        // STAR 7 local HTML application.
        // Data is stored in this app's WebView storage.
        // Secure production authentication and cloud
        // storage require a separate backend.

        web.loadUrl("file:///android_asset/index.html");
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

        if (
            requestCode == PICK_FILE &&
            fileCallback != null
        ) {

            fileCallback.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(
                    resultCode,
                    data
                )
            );

            fileCallback = null;
        }
    }

    @Override
    public void onBackPressed() {

        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {

        if (web != null) {
            web.destroy();
            web = null;
        }

        super.onDestroy();
    }
}
