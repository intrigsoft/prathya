package com.intrigsoft.prathya.core.fixtures;

import com.intrigsoft.prathya.annotations.NonContractual;

/**
 * Stub production class with method-level @NonContractual annotations.
 */
public class PartialNonContractualFixture {

    public String getContractualData() { return "important"; }

    @NonContractual(reason = "Generated getter")
    public String getGeneratedField() { return "generated"; }

    @NonContractual(reason = "Utility method")
    public void utilityMethod() {}

    public int compute(int x) { return x * 2; }
}
