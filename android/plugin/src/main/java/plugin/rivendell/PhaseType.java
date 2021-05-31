package plugin.rivendell;

public enum PhaseType {
    PHASE_INIT ("init"),
    PHASE_OPENED("opened"),
    PHASE_FAILED ("failed"),
    PHASE_CLOSED ("closed"),
    PHASE_REWARD ("reward");

    private final String value;

    PhaseType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
