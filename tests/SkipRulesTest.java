import com.example.hongguosafeskip.SkipRules;
public class SkipRulesTest {
    public static void main(String[] args) {
        require(SkipRules.shouldClick("跳过广告",false));
        require(SkipRules.shouldClick("跳过此广告",false));
        require(SkipRules.shouldClick("跳过",true));
        require(!SkipRules.shouldClick("跳过",false));
        require(!SkipRules.shouldClick("跳过片头",true));
        require(!SkipRules.shouldClick("继续观看",true));
        require(!SkipRules.shouldClick("立即领取",true));
        require(!SkipRules.shouldClick("关闭",true));
        require(SkipRules.shouldClick("关闭广告",false));
        require(SkipRules.shouldClick("Skip Ad",false));
        require(SkipRules.containsAdContext("广告剩余 2 秒"));
        require(!SkipRules.containsAdContext("第六集"));
        System.out.println("PASS 12 checks");
    }
    private static void require(boolean b) { if(!b)throw new AssertionError("Rule check failed"); }
}
