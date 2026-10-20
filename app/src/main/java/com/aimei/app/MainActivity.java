package com.aimei.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.net.http.SslError;
import android.os.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int BG = 0xFFFBF7F3, INK = 0xFF352B32, MUTED = 0xFF84747B;
    private static final int ROSE = 0xFFB65C79, SOFT = 0xFFF3DFE4;
    private static final int PICK = 100, CAMERA = 101;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private LinearLayout root;
    private WebView web;
    private PermissionRequest cameraRequest;
    private ValueCallback<Uri[]> fileCallback;
    private boolean pickerOpen;
    private int tab;
    private ProgressBar progress;
    private TextView webStatus, webError;
    private Uri selectedGarment;
    private View fullScreenView;
    private WebChromeClient.CustomViewCallback fullScreenCallback;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::handleBack);
        }
        render(state == null ? 0 : state.getInt("tab", 0));
    }

    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(radius));
        return background;
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(color);
        text.setTextSize(size);
        text.setLineSpacing(dp(3), 1f);
        if (bold) text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return text;
    }

    private TextView button(String value, boolean primary, Runnable action) {
        TextView text = label(value, 15, primary ? Color.WHITE : ROSE, true);
        text.setGravity(Gravity.CENTER);
        text.setMinHeight(dp(52));
        text.setPadding(dp(14), dp(12), dp(14), dp(12));
        text.setBackground(new RippleDrawable(ColorStateList.valueOf(0x2279445A),
            shape(primary ? ROSE : SOFT, 18), null));
        text.setOnClickListener(view -> action.run());
        text.setFocusable(true);
        return text;
    }

    private void space(LinearLayout parent, int height) {
        parent.addView(new View(this), new LinearLayout.LayoutParams(1, dp(height)));
    }

    private LinearLayout column(int padding) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(padding), dp(padding), dp(padding), dp(padding));
        return layout;
    }

    private void newRoot() {
        root = column(0);
        root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(root);
        root.requestApplyInsets();
    }

    private void render(int selected) {
        closeWeb();
        tab = selected;
        newRoot();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        LinearLayout content = column(24);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        String title = tab == 0 ? "爱美" : tab == 1 ? "私人衣橱" : "我的";
        content.addView(label(title, 29, INK, true));
        space(content, 7);
        content.addView(label(tab == 0 ? "穿上喜欢的，遇见新的自己"
            : tab == 1 ? "把心动的衣服，慢慢收集起来" : "让每一次试穿，都自在一点", 13, MUTED, false));
        space(content, 26);
        if (tab == 0) home(content);
        else if (tab == 1) wardrobe(content);
        else profile(content);
        navigation();
    }

    private void home(LinearLayout content) {
        LinearLayout hero = column(20);
        hero.setBackground(shape(0xFFF1E3DE, 28));
        hero.addView(label("ANYWEAR  /  LIVE TRY-ON", 11, ROSE, true));
        space(hero, 12);
        hero.addView(label("你的随身\n实时试衣间", 30, INK, true));
        MirrorArt art = new MirrorArt();
        art.setContentDescription("试衣镜与连衣裙插画");
        hero.addView(art, new LinearLayout.LayoutParams(-1, dp(220)));
        hero.addView(label("打开镜头，试一件喜欢的衣服。\n转身、抬手，也能看到穿着的样子。", 14, MUTED, false));
        space(hero, 18);
        hero.addView(button("开启实时试衣   →", true, this::showWeb));
        content.addView(hero);
        space(content, 24);
        content.addView(label("从这里开始", 19, INK, true));
        space(content, 12);
        tool(content, "01", "收藏我的衣服", "从相册添加，整理你的穿搭灵感", () -> render(1));
        space(content, 10);
        tool(content, "02", "第一次试衣", "摄像头、选衣服和常见问题", this::guide);
        space(content, 20);
        content.addView(label("实时试衣服务由 Anywear 提供，需要网络连接。", 11, MUTED, false));
    }

    private void tool(LinearLayout parent, String number, String title, String detail, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(17), dp(16), dp(17));
        row.setBackground(shape(Color.WHITE, 20));
        TextView badge = label(number, 14, ROSE, true);
        row.addView(badge, new LinearLayout.LayoutParams(dp(40), -2));
        LinearLayout words = column(0);
        words.addView(label(title, 15, INK, true));
        space(words, 5);
        words.addView(label(detail, 12, MUTED, false));
        row.addView(words, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(label("›", 24, ROSE, false));
        row.setOnClickListener(view -> action.run());
        row.setFocusable(true);
        row.setContentDescription(title + "，" + detail);
        parent.addView(row, new LinearLayout.LayoutParams(-1, -2));
    }

    private void navigation() {
        LinearLayout nav = new LinearLayout(this);
        nav.setPadding(dp(14), dp(10), dp(14), dp(12));
        String[] names = {"试衣", "衣橱", "我的"};
        for (int index = 0; index < names.length; index++) {
            final int destination = index;
            TextView item = button(names[index], tab == index, () -> render(destination));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(48), 1);
            params.setMargins(dp(4), 0, dp(4), 0);
            item.setSelected(tab == index);
            nav.addView(item, params);
        }
        root.addView(nav);
    }

    private void wardrobe(LinearLayout content) {
        content.addView(button("＋  从相册添加衣服", true, () -> pickImage(null)));
        space(content, 20);
        List<File> images = ImageStore.list(this);
        content.addView(label(images.size() + " 件收藏", 13, MUTED, false));
        if (images.isEmpty()) {
            space(content, 35);
            TextView empty = label("把喜欢的衣服放进来\n\n清晰的单件服装图片，更方便试穿。", 17, MUTED, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(35), 0, dp(35));
            empty.setBackground(shape(Color.WHITE, 24));
            content.addView(empty, new LinearLayout.LayoutParams(-1, -2));
            return;
        }
        for (int position = 0; position < images.size(); position += 2) {
            LinearLayout row = new LinearLayout(this);
            for (int index = position; index < Math.min(position + 2, images.size()); index++) {
                File file = images.get(index);
                LinearLayout card = column(8);
                card.setBackground(shape(Color.WHITE, 20));
                ImageView image = new ImageView(this);
                image.setImageBitmap(ImageStore.thumbnail(file, 420));
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                image.setClipToOutline(true);
                image.setBackground(shape(SOFT, 14));
                image.setContentDescription("衣橱服装图片 " + (index + 1));
                card.addView(image, new LinearLayout.LayoutParams(-1, dp(175)));
                TextView caption = label("收藏 " + (images.size() - index), 13, INK, true);
                caption.setPadding(dp(5), dp(10), 0, dp(6));
                card.addView(caption);
                card.setOnClickListener(view -> preview(file));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
                params.setMargins(0, dp(12), dp(8), 0);
                row.addView(card, params);
            }
            if (row.getChildCount() == 1) row.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
            content.addView(row);
        }
    }

    private void preview(File file) {
        ImageView image = new ImageView(this);
        image.setImageBitmap(ImageStore.thumbnail(file, 1400));
        image.setAdjustViewBounds(true);
        image.setPadding(dp(12), dp(12), dp(12), dp(12));
        new AlertDialog.Builder(this).setTitle("这件，也想试一试")
            .setView(image)
            .setPositiveButton("去试衣", (dialog, which) -> {
                selectedGarment = ImageStore.uri(file);
                showWeb();
                toast("网页选择上传服装时，可直接使用这件收藏");
            })
            .setNeutralButton("删除", (dialog, which) -> confirmDelete(file))
            .setNegativeButton("关闭", null).show();
    }

    private void confirmDelete(File file) {
        new AlertDialog.Builder(this).setMessage("从衣橱删除这张收藏图片？")
            .setNegativeButton("保留", null)
            .setPositiveButton("删除", (dialog, which) -> {
                if (file.delete()) render(1);
                else toast("删除失败，请重试");
            }).show();
    }

    private void profile(LinearLayout content) {
        LinearLayout card = column(20);
        card.setBackground(shape(SOFT, 24));
        card.addView(label("穿搭不设限", 24, INK, true));
        space(card, 10);
        card.addView(label("喜欢的风格，都值得试一试。", 14, MUTED, false));
        content.addView(card);
        space(content, 20);
        tool(content, "♡", "试衣指南", "从打开摄像头开始", this::guide);
        space(content, 12);
        tool(content, "⌁", "摄像头权限", cameraAllowed() ? "已允许使用摄像头" : "试衣时由你决定是否授权", () -> {
            if (cameraAllowed()) toast("摄像头权限已开启");
            else requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA);
        });
        space(content, 12);
        tool(content, "↗", "在浏览器中试衣", "内嵌页面遇到兼容问题时使用", () -> external(SitePolicy.TRY_ON_URL));
        space(content, 12);
        tool(content, "i", "隐私与服务", "了解图片、摄像头和服务提供方", this::privacy);
        space(content, 28);
        content.addView(label("爱美 0.6.0 · Android 测试版\n第三方应用，非 Anywear 官方客户端", 12, MUTED, false));
    }

    private void guide() {
        new AlertDialog.Builder(this).setTitle("开始你的第一次试衣")
            .setMessage("1. 点「开启实时试衣」。\n\n"
                + "2. 进入 Anywear 页面后，点 Enable camera，并允许摄像头权限。"
                + "若还在介绍页，先点 Try Example。\n\n"
                + "3. 选择页面提供的衣服。页面提供服装上传时，可以从爱美衣橱或相册选择。\n\n"
                + "4. 等待连接后缓慢转身、抬手，观察试穿效果。\n\n"
                + "建议面对光源、保持网络稳定。若内嵌页面打不开，可使用「浏览器」入口。")
            .setPositiveButton("开始试衣", (dialog, which) -> showWeb())
            .setNegativeButton("知道了", null).show();
    }

    private void privacy() {
        new AlertDialog.Builder(this).setTitle("隐私与服务")
            .setMessage("衣橱图片保存在本机的应用私有目录中，删除应用会一并清除。\n\n"
                + "实时试衣由 Anywear 提供。授权后，摄像头画面会交给该服务处理，"
                + "请先阅读它的隐私政策。爱美本身不录制或保存试衣视频。\n\n"
                + "网站请求摄像头时，仅向 Anywear 官方 HTTPS 页面开放摄像头。")
            .setPositiveButton("服务隐私政策", (dialog, which) ->
                external("https://docs.platform.decart.ai/resources/privacy-policy"))
            .setNegativeButton("关闭", null).show();
    }

    @SuppressWarnings("SetJavaScriptEnabled")
    private void showWeb() {
        closeWeb();
        newRoot();
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(8), dp(4), dp(8), dp(4));
        toolbar.addView(button("‹", false, () -> render(0)),
            new LinearLayout.LayoutParams(dp(48), dp(48)));
        webStatus = label("正在连接 Anywear…", 12, INK, true);
        webStatus.setGravity(Gravity.CENTER);
        toolbar.addView(webStatus, new LinearLayout.LayoutParams(0, dp(48), 1));
        toolbar.addView(button("刷新", false, () -> {
            if (web != null) { webError.setVisibility(View.GONE); web.reload(); }
        }), new LinearLayout.LayoutParams(dp(60), dp(48)));
        toolbar.addView(button("浏览器", false, () -> external(SitePolicy.TRY_ON_URL)),
            new LinearLayout.LayoutParams(dp(76), dp(48)));
        root.addView(toolbar);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(3)));
        FrameLayout stage = new FrameLayout(this);
        root.addView(stage, new LinearLayout.LayoutParams(-1, 0, 1));
        web = new WebView(this);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportMultipleWindows(false);
        CookieManager.getInstance().setAcceptCookie(true);
        web.setBackgroundColor(Color.WHITE);
        stage.addView(web, new FrameLayout.LayoutParams(-1, -1));
        webError = label("", 16, INK, false);
        webError.setGravity(Gravity.CENTER);
        webError.setPadding(dp(30), dp(30), dp(30), dp(30));
        webError.setBackgroundColor(BG);
        webError.setVisibility(View.GONE);
        stage.addView(webError, new FrameLayout.LayoutParams(-1, -1));
        web.setWebViewClient(new TryOnNavigation());
        web.setWebChromeClient(new TryOnChrome());
        TextView tip = label("点 Enable camera 开启镜头 · 返回首页会结束当前试衣", 11, MUTED, false);
        tip.setGravity(Gravity.CENTER);
        tip.setPadding(dp(10), dp(10), dp(10), dp(10));
        root.addView(tip);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        web.loadUrl(SitePolicy.TRY_ON_URL);
    }

    private final class TryOnNavigation extends WebViewClient {
        @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            if (SitePolicy.isTrusted(url)) return false;
            if (request.isForMainFrame() && request.hasGesture()) external(url);
            return true;
        }
        @Override public void onPageFinished(WebView view, String url) {
            if (web != view) return;
            webStatus.setText("Anywear · 实时试衣");
            progress.setVisibility(View.GONE);
        }
        @Override public void onReceivedError(WebView view, WebResourceRequest request,
                                               WebResourceError error) {
            if (request.isForMainFrame()) connectionError("暂时无法连接试衣服务\n\n检查网络后点「刷新」，或试试「浏览器」。");
        }
        @Override public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.cancel();
            connectionError("服务的安全连接验证失败\n\n请检查手机日期和网络，稍后重试。");
        }
        @Override public void onReceivedHttpError(WebView view, WebResourceRequest request,
                                                   WebResourceResponse response) {
            if (request.isForMainFrame() && response.getStatusCode() >= 400) {
                connectionError("试衣服务暂时不可用（" + response.getStatusCode() + "）\n\n请稍后刷新。");
            }
        }
    }

    private final class TryOnChrome extends WebChromeClient {
        @Override public void onProgressChanged(WebView view, int value) {
            if (web != view) return;
            progress.setProgress(value);
            progress.setVisibility(value == 100 ? View.GONE : View.VISIBLE);
        }
        @Override public void onPermissionRequest(PermissionRequest request) {
            runOnUiThread(() -> cameraPermission(request));
        }
        @Override public void onPermissionRequestCanceled(PermissionRequest request) {
            if (cameraRequest == request) cameraRequest = null;
        }
        @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                                    FileChooserParams params) {
            if (!SitePolicy.isTrusted(view.getUrl())) { callback.onReceiveValue(null); return true; }
            if (fileCallback != null) fileCallback.onReceiveValue(null);
            fileCallback = callback;
            if (selectedGarment != null) {
                Uri chosen = selectedGarment;
                selectedGarment = null;
                completeFileChoice(new Uri[]{chosen});
            } else chooseFileSource();
            return true;
        }
        @Override public void onShowCustomView(View view, CustomViewCallback callback) {
            if (fullScreenView != null) { callback.onCustomViewHidden(); return; }
            fullScreenView = view;
            fullScreenCallback = callback;
            addContentView(view, new android.view.ViewGroup.LayoutParams(-1, -1));
        }
        @Override public void onHideCustomView() { hideFullScreen(); }
    }

    private void cameraPermission(PermissionRequest request) {
        boolean video = Arrays.asList(request.getResources()).contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE);
        if (web == null || !SitePolicy.isTrusted(web.getUrl())
                || !SitePolicy.isTrusted(request.getOrigin().toString()) || !video) {
            request.deny();
            return;
        }
        if (cameraRequest != null) cameraRequest.deny();
        cameraRequest = request;
        if (cameraAllowed()) grantCamera();
        else requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA);
    }

    private boolean cameraAllowed() {
        return checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void grantCamera() {
        PermissionRequest request = cameraRequest;
        cameraRequest = null;
        if (request == null) return;
        if (web != null && SitePolicy.isTrusted(web.getUrl()) && cameraAllowed()) {
            request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
        } else request.deny();
    }

    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code == CAMERA) {
            grantCamera();
            if (!cameraAllowed()) toast("未开启摄像头权限，可在系统应用设置中开启");
        }
    }

    private void chooseFileSource() {
        new AlertDialog.Builder(this).setTitle("选择一件衣服")
            .setItems(new String[]{"从爱美衣橱选择", "从手机相册选择"}, (dialog, which) -> {
                if (which == 0) chooseWardrobe();
                else pickImage(fileCallback);
            }).setOnCancelListener(dialog -> completeFileChoice(null)).show();
    }

    private void chooseWardrobe() {
        List<File> items = ImageStore.list(this);
        if (items.isEmpty()) { pickImage(fileCallback); return; }
        String[] names = new String[items.size()];
        for (int index = 0; index < items.size(); index++) names[index] = "收藏 " + (items.size() - index);
        new AlertDialog.Builder(this).setTitle("我的衣橱")
            .setItems(names, (dialog, which) -> completeFileChoice(new Uri[]{ImageStore.uri(items.get(which))}))
            .setOnCancelListener(dialog -> completeFileChoice(null)).show();
    }

    private void pickImage(ValueCallback<Uri[]> callback) {
        if (callback != null) fileCallback = callback;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        pickerOpen = true;
        try { startActivityForResult(intent, PICK); }
        catch (ActivityNotFoundException error) {
            pickerOpen = false;
            completeFileChoice(null);
            toast("没有可用的图片选择器");
        }
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK) return;
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            pickerOpen = false;
            completeFileChoice(null);
            return;
        }
        Uri source = data.getData();
        toast("正在保存图片…");
        worker.execute(() -> {
            try {
                File saved = ImageStore.save(this, source);
                runOnUiThread(() -> {
                    pickerOpen = false;
                    if (isFinishing() || isDestroyed()) return;
                    if (fileCallback != null) completeFileChoice(new Uri[]{ImageStore.uri(saved)});
                    else render(1);
                    toast("已加入私人衣橱");
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    pickerOpen = false;
                    completeFileChoice(null);
                    if (!isFinishing() && !isDestroyed()) toast("添加失败：" + error.getMessage());
                });
            }
        });
    }

    private void completeFileChoice(Uri[] result) {
        ValueCallback<Uri[]> callback = fileCallback;
        fileCallback = null;
        if (callback != null) callback.onReceiveValue(result);
    }

    private void connectionError(String message) {
        if (web == null || webError == null) return;
        progress.setVisibility(View.GONE);
        webStatus.setText("连接未完成");
        webError.setText(message);
        webError.setVisibility(View.VISIBLE);
    }

    private void external(String address) {
        if (!SitePolicy.isExternalWeb(address)) { toast("此链接无法打开"); return; }
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(address))); }
        catch (ActivityNotFoundException error) { toast("请先安装可用的浏览器"); }
    }

    private void hideFullScreen() {
        if (fullScreenView != null && fullScreenView.getParent() instanceof android.view.ViewGroup) {
            ((android.view.ViewGroup) fullScreenView.getParent()).removeView(fullScreenView);
        }
        fullScreenView = null;
        if (fullScreenCallback != null) {
            WebChromeClient.CustomViewCallback callback = fullScreenCallback;
            fullScreenCallback = null;
            callback.onCustomViewHidden();
        }
    }

    private void closeWeb() {
        hideFullScreen();
        if (cameraRequest != null) { cameraRequest.deny(); cameraRequest = null; }
        completeFileChoice(null);
        if (web == null) return;
        WebView closing = web;
        web = null;
        closing.stopLoading();
        closing.loadUrl("about:blank");
        if (closing.getParent() instanceof android.view.ViewGroup) {
            ((android.view.ViewGroup) closing.getParent()).removeView(closing);
        }
        closing.destroy();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void handleBack() {
        if (fullScreenView != null) { hideFullScreen(); return; }
        if (web != null) { render(0); return; }
        if (tab != 0) { render(0); return; }
        moveTaskToBack(true);
    }

    @Override public void onBackPressed() { handleBack(); }
    @Override public void onSaveInstanceState(Bundle state) {
        state.putInt("tab", tab);
        super.onSaveInstanceState(state);
    }
    @Override public void onResume() {
        super.onResume();
        if (web != null) web.onResume();
    }
    @Override public void onPause() {
        if (web != null) web.onPause();
        super.onPause();
    }
    @Override public void onStop() {
        if (web != null && !pickerOpen) render(0);
        super.onStop();
    }
    @Override public void onDestroy() {
        closeWeb();
        worker.shutdown();
        super.onDestroy();
    }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }

    private final class MirrorArt extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        MirrorArt() { super(MainActivity.this); }
        private void color(Canvas canvas, int color, RectF rectangle, float radius) {
            paint.setColor(color);
            canvas.drawRoundRect(rectangle, radius, radius, paint);
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.save();
            canvas.scale(getWidth() / 300f, getHeight() / 220f);
            color(canvas, 0xFFDBC9BC, new RectF(87, 4, 233, 214), 73);
            color(canvas, 0xFFFDF8F1, new RectF(93, 10, 227, 208), 67);
            paint.setColor(0xFFE8D1D9);
            canvas.drawCircle(252, 76, 29, paint);
            paint.setColor(0xFF8D6473);
            canvas.drawOval(140, 36, 181, 91, paint);
            paint.setColor(0xFFE6BDAA);
            canvas.drawOval(148, 43, 175, 81, paint);
            color(canvas, 0xFFE6BDAA, new RectF(155, 72, 168, 95), 6);
            Path dress = new Path();
            dress.moveTo(143, 88); dress.lineTo(156, 94); dress.lineTo(166, 94);
            dress.lineTo(179, 88); dress.lineTo(186, 112); dress.lineTo(173, 121);
            dress.lineTo(196, 186); dress.quadTo(160, 204, 124, 186);
            dress.lineTo(147, 121); dress.lineTo(134, 112); dress.close();
            paint.setColor(ROSE);
            canvas.drawPath(dress, paint);
            paint.setColor(0xFFCF8DA2);
            canvas.drawRect(148, 119, 173, 124, paint);
            color(canvas, 0xFFE6BDAA, new RectF(131, 108, 140, 151), 5);
            color(canvas, 0xFFE6BDAA, new RectF(181, 108, 190, 151), 5);
            paint.setColor(0xFFBCA388);
            canvas.drawCircle(49, 153, 19, paint);
            paint.setColor(0xFFDCC5B2);
            canvas.drawOval(24, 171, 75, 202, paint);
            paint.setColor(0xFFB65C79);
            Path star = new Path();
            star.moveTo(251, 14); star.lineTo(255, 26); star.lineTo(267, 30);
            star.lineTo(255, 34); star.lineTo(251, 46); star.lineTo(247, 34);
            star.lineTo(235, 30); star.lineTo(247, 26); star.close();
            canvas.drawPath(star, paint);
            canvas.restore();
        }
    }
}
