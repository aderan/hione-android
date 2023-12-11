package io.agora.board.fast.sample;

import android.app.Application;
import android.webkit.WebView;
import com.herewhite.sdk.WhiteboardView;

public class MainApplication extends Application {

    private WhiteboardView whiteboardView;

    @Override
    public void onCreate() {
        super.onCreate();

        WebView.setWebContentsDebuggingEnabled(true);
        // WhiteboardView.setEntryUrl("file:///android_asset/whiteboard2/index.html");
        // whiteboardView = new WhiteboardView(this);
    }
}
