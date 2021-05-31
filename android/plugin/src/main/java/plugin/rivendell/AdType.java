package plugin.rivendell;

public enum AdType {
    BANNER("banner"),
    INTERSTITIAL("interstitial"),
    REWARDED_VIDEO("rewardedVideo");

    private final String value;

    AdType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
