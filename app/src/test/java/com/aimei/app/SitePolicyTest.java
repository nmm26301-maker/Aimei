package com.aimei.app;

import org.junit.Test;
import static org.junit.Assert.*;

public class SitePolicyTest {
    @Test public void acceptsOfficialMobileEntry() {
        assertTrue(SitePolicy.isTrusted(SitePolicy.TRY_ON_URL));
        assertTrue(SitePolicy.isTrusted("https://anywear.decart.ai:443/"));
    }
    @Test public void rejectsLookalikeHosts() {
        assertFalse(SitePolicy.isTrusted("https://anywear.decart.ai.evil.example/"));
        assertFalse(SitePolicy.isTrusted("https://evil-anywear.decart.ai/"));
        assertFalse(SitePolicy.isTrusted("https://anywear.decart.ai@evil.example/"));
    }
    @Test public void rejectsInsecureAndLocalOrigins() {
        assertFalse(SitePolicy.isTrusted("http://anywear.decart.ai/"));
        assertFalse(SitePolicy.isTrusted("file:///android_asset/index.html"));
        assertFalse(SitePolicy.isTrusted("https://anywear.decart.ai:8443/"));
        assertFalse(SitePolicy.isTrusted(null));
    }
    @Test public void externalNavigationAllowsOnlyHttps() {
        assertTrue(SitePolicy.isExternalWeb("https://docs.platform.decart.ai/"));
        assertFalse(SitePolicy.isExternalWeb("javascript:alert(1)"));
        assertFalse(SitePolicy.isExternalWeb("intent://example"));
        assertFalse(SitePolicy.isExternalWeb("https://user:password@example.com/"));
    }
}
