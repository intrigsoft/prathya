package dev.pactum.core.report;

import dev.pactum.core.model.CoverageMatrix;
import dev.pactum.core.model.Violation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface ReportWriter {

    void writeJsonReport(CoverageMatrix matrix, List<Violation> violations, Path outputFile) throws IOException;

    void writeHtmlReport(CoverageMatrix matrix, List<Violation> violations, Path outputDir) throws IOException;
}
