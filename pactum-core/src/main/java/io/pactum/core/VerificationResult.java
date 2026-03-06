package io.pactum.core;

import java.util.Collections;
import java.util.List;

/**
 * Holds the result of a pactum verification run.
 */
public class VerificationResult {

    private final List<String> passed;
    private final List<String> failed;

    public VerificationResult(List<String> passed, List<String> failed) {
        this.passed = Collections.unmodifiableList(passed);
        this.failed = Collections.unmodifiableList(failed);
    }

    /** Returns the list of pacts/requirements that were successfully verified. */
    public List<String> getPassed() {
        return passed;
    }

    /** Returns the list of pacts/requirements that failed verification. */
    public List<String> getFailed() {
        return failed;
    }

    /** Returns {@code true} if all verifications passed with no failures. */
    public boolean isSuccess() {
        return failed.isEmpty();
    }

    @Override
    public String toString() {
        return "VerificationResult{passed=" + passed.size() + ", failed=" + failed.size() + "}";
    }
}
