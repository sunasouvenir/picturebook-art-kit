package com.sunasouvenir.artkit;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/** 그림책 미술키트: 앱 안에 담긴 웹 화면(assets/www)을 인터넷 없이 보여 줘요. */
public class MainActivity extends Activity {
    private WebView web;
    private ValueCallback<Uri[]> pickCb;
    private static final int PICK = 7;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;

        // 상단 바: 홈 화면 머리 색(밝을 때 피치, 어두울 때 짙은 갈색)에 맞춰요
        Window w = getWindow();
        w.setStatusBarColor(night ? 0xFF4A3833 : 0xFFF9D5C2);
        w.setNavigationBarColor(night ? 0xFF1A1F2C : 0xFFFFFFFF);
        if (!night) {
            int flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            w.getDecorView().setSystemUiVisibility(flags);
        }

        web = new WebView(this);
        web.setBackgroundColor(night ? 0xFF1A1F2C : 0xFFFFFFFF);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setSupportMultipleWindows(false);
        s.setTextZoom(100);

        web.addJavascriptInterface(new Bridge(), "AndroidBridge");
        web.setWebChromeClient(new WebChromeClient() {
            // 키트 사진 넣기: 휴대폰 사진첩(갤러리)을 열어요
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (pickCb != null) pickCb.onReceiveValue(null);
                pickCb = cb;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE);
                try {
                    startActivityForResult(Intent.createChooser(i, "사진 고르기"), PICK);
                } catch (Exception e) {
                    pickCb = null;
                    toast("사진첩을 열지 못했어요");
                    return false;
                }
                return true;
            }
        });
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                return openOutside(req.getUrl());
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return openOutside(Uri.parse(url));
            }
        });

        if (saved != null) web.restoreState(saved);
        if (web.getUrl() == null) web.loadUrl("file:///android_asset/www/index.html");
    }

    /** 유튜브 같은 바깥 링크는 휴대폰의 다른 앱(브라우저·유튜브)으로 열어요. */
    private boolean openOutside(Uri uri) {
        if ("file".equals(uri.getScheme())) return false;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception ignored) {
        }
        return true;
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req != PICK) {
            super.onActivityResult(req, res, data);
            return;
        }
        Uri[] out = null;
        if (res == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int n = data.getClipData().getItemCount();
                out = new Uri[n];
                for (int k = 0; k < n; k++) out[k] = data.getClipData().getItemAt(k).getUri();
            } else if (data.getData() != null) {
                out = new Uri[]{data.getData()};
            }
        }
        if (pickCb != null) pickCb.onReceiveValue(out);
        pickCb = null;
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    /** 뒤로 가기: 앱 안 화면을 먼저 되돌리고, 첫 화면이면 앱을 닫아요. */
    @Override
    public void onBackPressed() {
        web.evaluateJavascript("(window.__androidBack?window.__androidBack():false)", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String handled) {
                if (!"true".equals(handled)) finish();
            }
        });
    }

    public class Bridge {
        @JavascriptInterface
        public void saveFile(final String name, final String base64) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    save(name, base64);
                }
            });
        }
    }

    /** 수업 계획서 같은 파일을 휴대폰 '다운로드' 폴더에 저장하고 바로 열 수 있게 해요. */
    private void save(String name, String base64) {
        try {
            byte[] data = Base64.decode(base64, Base64.DEFAULT);
            String mime = name.endsWith(".docx")
                    ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    : "application/octet-stream";
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues cv = new ContentValues();
                cv.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                cv.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                cv.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                OutputStream os = getContentResolver().openOutputStream(uri);
                os.write(data);
                os.close();
                toast("다운로드 폴더에 저장했어요\n" + name);
                Intent open = new Intent(Intent.ACTION_VIEW);
                open.setDataAndType(uri, mime);
                open.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try {
                    startActivity(Intent.createChooser(open, "계획서 열기"));
                } catch (Exception ignored) {
                }
            } else {
                File f = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), name);
                FileOutputStream fo = new FileOutputStream(f);
                fo.write(data);
                fo.close();
                toast("저장했어요\n" + f.getAbsolutePath());
            }
        } catch (Exception e) {
            toast("파일을 저장하지 못했어요");
        }
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
