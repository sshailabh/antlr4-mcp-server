package com.github.sshailabh.antlr4mcp.codegen;

/**
 * Enumeration of supported ANTLR4 target languages
 */
public enum TargetLanguage {
    JAVA("Java", "Java", ".java"),
    PYTHON3("Python3", "Python3", ".py"),
    JAVASCRIPT("JavaScript", "JavaScript", ".js"),
    TYPESCRIPT("TypeScript", "TypeScript", ".ts"),
    CPP("Cpp", "Cpp", ".cpp"),
    CSHARP("CSharp", "CSharp", ".cs"),
    GO("Go", "Go", ".go"),
    SWIFT("Swift", "Swift", ".swift"),
    PHP("PHP", "PHP", ".php"),
    DART("Dart", "Dart", ".dart");

    private final String antlrName;
    private final String displayName;
    private final String fileExtension;

    TargetLanguage(String antlrName, String displayName, String fileExtension) {
        this.antlrName = antlrName;
        this.displayName = displayName;
        this.fileExtension = fileExtension;
    }

    public String getAntlrName() {
        return antlrName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    /**
     * Parse target language from string
     */
    public static TargetLanguage fromString(String value) {
        if (value == null) {
            return JAVA; // default
        }

        String normalized = value.trim().toLowerCase();

        for (TargetLanguage target : values()) {
            if (target.displayName.toLowerCase().equals(normalized) ||
                target.name().toLowerCase().equals(normalized) ||
                target.antlrName.toLowerCase().equals(normalized)) {
                return target;
            }
        }

        throw new IllegalArgumentException("Unknown target language: " + value);
    }

    /**
     * Check if this is a strongly-typed language
     */
    public boolean isStronglyTyped() {
        return this == JAVA || this == CPP || this == CSHARP ||
               this == SWIFT || this == TYPESCRIPT || this == DART;
    }

    /**
     * Check if this language uses garbage collection
     */
    public boolean isGarbageCollected() {
        return this != CPP;
    }

    /**
     * Get runtime library import/package for this language
     */
    public String getRuntimeImport() {
        switch (this) {
            case JAVA:
                return "org.antlr.v4.runtime";
            case PYTHON3:
                return "antlr4";
            case JAVASCRIPT:
            case TYPESCRIPT:
                return "antlr4";
            case CPP:
                return "antlr4-runtime";
            case CSHARP:
                return "Antlr4.Runtime";
            case GO:
                return "github.com/antlr/antlr4/runtime/Go/antlr";
            case SWIFT:
                return "Antlr4";
            case PHP:
                return "Antlr\\Antlr4\\Runtime";
            case DART:
                return "package:antlr4/antlr4.dart";
            default:
                return "";
        }
    }
}
