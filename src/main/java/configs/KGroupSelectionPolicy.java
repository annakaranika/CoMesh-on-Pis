package configs;

public enum KGroupSelectionPolicy {
    RANDOM("rs"),
    LSHMIX("ls"),
    STATIC("st"),
    ;

    public String name;

    KGroupSelectionPolicy(String name) { this.name = name; }
}
