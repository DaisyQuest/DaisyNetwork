package dev.daisynetwork.proxy;

public enum WafSeverity {
    NOTICE(2),
    WARNING(3),
    ERROR(4),
    CRITICAL(5);

    private final int anomalyScore;

    WafSeverity(int anomalyScore) {
        this.anomalyScore = anomalyScore;
    }

    public int anomalyScore() {
        return anomalyScore;
    }
}
