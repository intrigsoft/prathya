package com.intrigsoft.prathya.maven;

import com.intrigsoft.prathya.core.model.CodeCoverageSummary;
import com.intrigsoft.prathya.core.model.NonContractualEntry;
import org.jacoco.core.analysis.*;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;
import org.jacoco.core.tools.ExecFileLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Computes {@link CodeCoverageSummary} directly from a JaCoCo {@code .exec} file and
 * compiled class files — no XML intermediate, no child process.
 * <p>
 * {@code @NonContractual} exclusions are applied by skipping the matching
 * {@link IClassCoverage}/{@link IMethodCoverage} objects before accumulating counters,
 * rather than via post-hoc XML counter subtraction.
 */
public class JacocoCoverageAnalyzer {

    /**
     * Analyzes the given exec file against the class files in {@code classesDir},
     * applying {@code exclusions} before summing counters.
     *
     * @param execFile   path to {@code jacoco.exec} written by the JaCoCo agent
     * @param classesDir directory containing compiled {@code .class} files
     * @param exclusions {@code @NonContractual} entries to exclude from counters
     * @return aggregated coverage summary
     * @throws IOException if the exec file or class files cannot be read
     */
    public CodeCoverageSummary analyze(Path execFile, Path classesDir,
                                       List<NonContractualEntry> exclusions) throws IOException {
        // 1. Load exec file
        ExecFileLoader loader = new ExecFileLoader();
        loader.load(execFile.toFile());
        ExecutionDataStore executionData = loader.getExecutionDataStore();
        SessionInfoStore sessionInfo = loader.getSessionInfoStore();

        // 2. Analyze class files
        CoverageBuilder coverageBuilder = new CoverageBuilder();
        Analyzer analyzer = new Analyzer(executionData, coverageBuilder);
        analyzer.analyzeAll(classesDir.toFile());

        // 3. Build exclusion sets (FQN → internal name: replace '.' with '/')
        Set<String> excludedClasses = new HashSet<>();
        Map<String, Set<String>> excludedMethods = new HashMap<>();
        for (NonContractualEntry entry : exclusions) {
            String internalName = entry.getClassName().replace('.', '/');
            if (entry.getMethodName() == null) {
                excludedClasses.add(internalName);
            } else {
                excludedMethods.computeIfAbsent(internalName, k -> new HashSet<>())
                               .add(entry.getMethodName());
            }
        }

        // 4. Traverse coverage model and accumulate counters
        int lineCovered = 0, lineMissed = 0;
        int branchCovered = 0, branchMissed = 0;
        int methodCovered = 0, methodMissed = 0;
        int classCovered = 0, classMissed = 0;

        IBundleCoverage bundle = coverageBuilder.getBundle("Prathya");
        for (IPackageCoverage pkg : bundle.getPackages()) {
            for (IClassCoverage cls : pkg.getClasses()) {
                String className = cls.getName(); // already in internal form

                if (excludedClasses.contains(className)) {
                    continue;
                }

                // Accumulate class-level counter
                ICounter classCounter = cls.getClassCounter();
                classCovered += classCounter.getCoveredCount();
                classMissed  += classCounter.getMissedCount();

                Set<String> excludedMethodNames = excludedMethods.getOrDefault(className, Set.of());

                for (IMethodCoverage method : cls.getMethods()) {
                    if (excludedMethodNames.contains(method.getName())) {
                        continue;
                    }

                    ICounter lines = method.getLineCounter();
                    lineCovered  += lines.getCoveredCount();
                    lineMissed   += lines.getMissedCount();

                    ICounter branches = method.getBranchCounter();
                    branchCovered  += branches.getCoveredCount();
                    branchMissed   += branches.getMissedCount();

                    ICounter methods = method.getMethodCounter();
                    methodCovered  += methods.getCoveredCount();
                    methodMissed   += methods.getMissedCount();
                }
            }
        }

        return new CodeCoverageSummary(
                lineCovered, lineMissed,
                branchCovered, branchMissed,
                methodCovered, methodMissed,
                classCovered, classMissed);
    }
}
