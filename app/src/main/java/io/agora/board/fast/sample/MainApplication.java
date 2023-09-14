package io.agora.board.fast.sample;

import android.app.Application;
import android.webkit.WebView;

public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        WebView.setWebContentsDebuggingEnabled(true);
    }
}
