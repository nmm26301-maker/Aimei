package com.example.hongguosafeskip;

import android.accessibilityservice.AccessibilityService;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** 只操作红果界面上已显示且可点击的跳过按钮。 */
public final class SafeSkipService extends AccessibilityService {
    private static final String TARGET="com.phoenix.read";
    private static final long COOLDOWN=2000L;
    private static final int MAX_NODES=250;
    private long lastClick=-COOLDOWN;

    /** @brief 仅处理目标窗口的事件。 */
    @Override public void onAccessibilityEvent(AccessibilityEvent e) {
        if(e==null || e.getPackageName()==null || !TARGET.contentEquals(e.getPackageName()))return;
        if(!getSharedPreferences("config",MODE_PRIVATE).getBoolean("enabled",false))return;
        long now=SystemClock.uptimeMillis();
        if(now-lastClick<COOLDOWN)return;
        AccessibilityNodeInfo root=getRootInActiveWindow();
        if(root==null || root.getPackageName()==null || !TARGET.contentEquals(root.getPackageName()))return;
        scanAndClick(root,now);
    }

    /** @brief 查找符合条件的可见控件。 */
    private void scanAndClick(AccessibilityNodeInfo root,long now) {
        ArrayDeque<AccessibilityNodeInfo> pending=new ArrayDeque<>();
        List<AccessibilityNodeInfo> candidates=new ArrayList<>();
        pending.add(root);
        boolean adContext=false;
        int count=0;
        while(!pending.isEmpty() && count++<MAX_NODES) {
            AccessibilityNodeInfo n=pending.removeFirst();
            if(!n.isVisibleToUser())continue;
            String t=n.getText()==null?"":n.getText().toString();
            String d=n.getContentDescription()==null?"":n.getContentDescription().toString();
            adContext|=SkipRules.containsAdContext(t)||SkipRules.containsAdContext(d);
            if(SkipRules.isExplicitAdSkip(t)||SkipRules.isExplicitAdSkip(d)
                ||SkipRules.isGenericSkip(t)||SkipRules.isGenericSkip(d))candidates.add(n);
            for(int i=0;i<n.getChildCount() && pending.size()<MAX_NODES;i++) {
                AccessibilityNodeInfo child=n.getChild(i);
                if(child!=null)pending.add(child);
            }
        }
        for(AccessibilityNodeInfo n:candidates) {
            String t=n.getText()==null?"":n.getText().toString();
            String d=n.getContentDescription()==null?"":n.getContentDescription().toString();
            if((SkipRules.shouldClick(t,adContext)||SkipRules.shouldClick(d,adContext))
                && clickCandidate(n)) { lastClick=now; return; }
        }
    }

    /** @return 本节点或较近祖先是否成功执行点击。 */
    private boolean clickCandidate(AccessibilityNodeInfo n) {
        AccessibilityNodeInfo current=n;
        for(int depth=0;depth<=3 && current!=null;depth++) {
            if(current.isVisibleToUser() && current.isEnabled() && current.isClickable())
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            current=current.getParent();
        }
        return false;
    }

    /** @brief 系统中断时不执行其他操作。 */
    @Override public void onInterrupt(){}
}
