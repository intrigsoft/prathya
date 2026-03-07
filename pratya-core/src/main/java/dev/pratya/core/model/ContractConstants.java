package dev.pratya.core.model;

import java.util.regex.Pattern;

/**
 * Shared ID-format constants used by both the parser and the mutator.
 */
public final class ContractConstants {

    /** Requirement ID pattern: {MODULE}-{SEQ}, e.g. ORD-001. */
    public static final Pattern REQ_ID_PATTERN = Pattern.compile("^[A-Z]+-\\d+$");

    /** Corner-case ID pattern: {MODULE}-{SEQ}-CC-{N}, e.g. ORD-001-CC-001. */
    public static final Pattern CC_ID_PATTERN = Pattern.compile("^[A-Z]+-\\d+-CC-\\d+$");

    private ContractConstants() {}
}
