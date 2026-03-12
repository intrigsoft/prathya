package com.intrigsoft.prathya.core.fixtures;

import com.intrigsoft.prathya.annotations.NonContractual;

/**
 * Stub production class used as a fixture for NonContractualScanner tests.
 */
@NonContractual(reason = "DTO class")
public class NonContractualFixture {

    public String getName() { return "fixture"; }

    public int getValue() { return 42; }
}
