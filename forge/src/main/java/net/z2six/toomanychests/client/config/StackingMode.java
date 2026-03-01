package net.z2six.toomanychests.client.config;

public enum StackingMode {
    ALL_IDENTICAL("All identical items"),
    STACKABLE_ONLY("Only stackable items"),
    NONE("No stacking");

    private final String label;

    StackingMode(String label) {
        this.label = label;
    }

    public String label() {
        return this.label;
    }

    public StackingMode next() {
        StackingMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
