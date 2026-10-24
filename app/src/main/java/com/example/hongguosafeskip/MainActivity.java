package com.example.hongguosafeskip;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.accessibility.AccessibilityManager;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import java.util.List;

/** 无联网权限的本地控制页面。 */
public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(247,248,252);
    private static final int INK = Color.rgb(27,33,48);
    private static final int MUTED = Color.rgb(107,116,137);
    private static final int BLUE = Color.rgb(66,92,226);
    private TextView status;

    /** @brief 生成单页配置界面。 */
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0x2000);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(1);
        layout.setPadding(dp(22),dp(32),dp(22),dp(26));
        scroll.addView(layout);
        setContentView(scroll);

        layout.addView(text("HONGGUO  ·  SAFE SKIP",12,BLUE,true));
        TextView heading = text("红果安心跳",32,INK,true);
        heading.setPadding(0,dp(18),0,dp(8));
        layout.addView(heading);
        layout.addView(text("广告出现可用的跳过按钮时，帮你自动点击。",15,MUTED,false));

        LinearLayout controls = card();
        addTop(layout, controls, 28);
        controls.addView(text("自动跳过广告",21,INK,true));
        Switch toggle = new Switch(this);
        toggle.setText("启用红果专属辅助");
        toggle.setTextSize(16);
        toggle.setTextColor(INK);
        toggle.setPadding(0,dp(22),0,dp(22));
        toggle.setChecked(getSharedPreferences("config",MODE_PRIVATE).getBoolean("enabled",false));
        toggle.setOnCheckedChangeListener((button,enabled) ->
            getSharedPreferences("config",MODE_PRIVATE).edit().putBoolean("enabled",enabled).apply());
        controls.addView(toggle);
        status = text("正在检查系统授权…",14,MUTED,false);
        controls.addView(status);
        Button permissions = new Button(this);
        permissions.setText("打开无障碍授权");
        permissions.setAllCaps(false);
        permissions.setTextSize(15);
        permissions.setTextColor(Color.WHITE);
        permissions.setBackground(round(BLUE,14));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,dp(54));
        p.topMargin=dp(18);
        controls.addView(permissions,p);
        permissions.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        LinearLayout details = card();
        addTop(layout,details,16);
        details.addView(text("安心模式",19,INK,true));
        TextView info=text("✓ 仅在红果短剧中运行\n✓ 不需要 Root\n✓ 不使用网络、位置或相册权限\n✓ 明确出现广告跳过按钮才会点击\n✓ 2 秒防重复点击\n× 不跳过强制观看的倒计时广告\n× 不自动滑动、不开通会员或解锁付费内容",14,MUTED,false);
        info.setPadding(0,dp(14),0,0);
        info.setLineSpacing(dp(7),1);
        details.addView(info);
        TextView footer=text("先打开开关，再允许无障碍服务。如果安卓提示“受限设置”，请在系统应用信息中针对本应用允许受限设置。仅向自己信任的应用开放无障碍权限。",13,MUTED,false);
        footer.setPadding(dp(4),dp(24),dp(4),0);
        layout.addView(footer);
    }

    /** @brief 页面返回前台时更新授权状态。 */
    @Override public void onResume() {
        super.onResume();
        if (status==null) return;
        boolean ok=isAuthorized();
        status.setText(ok?"● 无障碍服务已授权":"○ 尚未授权无障碍服务");
        status.setTextColor(ok?Color.rgb(29,128,88):MUTED);
    }

    /** @return 无障碍服务是否被系统授权。 */
    private boolean isAuthorized() {
        AccessibilityManager m=(AccessibilityManager)getSystemService(ACCESSIBILITY_SERVICE);
        if(m==null)return false;
        List<AccessibilityServiceInfo> enabled=m.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for(AccessibilityServiceInfo service:enabled) {
            if(service.getResolveInfo()!=null && service.getResolveInfo().serviceInfo!=null
                && getPackageName().equals(service.getResolveInfo().serviceInfo.packageName)
                && SafeSkipService.class.getName().equals(service.getResolveInfo().serviceInfo.name)) return true;
        }
        return false;
    }

    /** @return 配置卡片。 */
    private LinearLayout card() {
        LinearLayout v=new LinearLayout(this);
        v.setOrientation(1);
        v.setPadding(dp(20),dp(22),dp(20),dp(22));
        v.setBackground(round(Color.WHITE,20));
        v.setElevation(dp(2));
        return v;
    }

    /** @return TextView 实例。 */
    private TextView text(String value,int sp,int color,boolean bold) {
        TextView v=new TextView(this);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setGravity(Gravity.START);
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return v;
    }

    /** @brief 添加顶部间距。 */
    private void addTop(LinearLayout parent,android.view.View child,int top) {
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.topMargin=dp(top);
        parent.addView(child,p);
    }

    /** @return 圆角背景。 */
    private GradientDrawable round(int color,float radius) {
        GradientDrawable d=new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp((int)radius));
        return d;
    }

    /** @return dp 对应的像素数。 */
    private int dp(int value) {
        return Math.round(value*getResources().getDisplayMetrics().density);
    }
}
