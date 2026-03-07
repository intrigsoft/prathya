package dev.prathya.core.audit;

import dev.prathya.core.model.*;

import java.util.*;

/**
 * Implements audit rules from the spec:
 * <ul>
 *   <li>ORPHANED_ANNOTATION — annotation ID not found in YAML</li>
 *   <li>UNCOVERED_REQUIREMENT — approved requirement with no tests</li>
 *   <li>UNCOVERED_CORNER_CASE — approved requirement with uncovered corner cases</li>
 *   <li>DEPRECATED_REFERENCE — deprecated requirement still referenced</li>
 *   <li>SUPERSEDED_REFERENCE — superseded requirement still referenced</li>
 * </ul>
 */
public class DefaultAuditEngine implements AuditEngine {

    @Override
    public List<Violation> audit(ModuleContract contract, List<TraceEntry> traces) {
        List<Violation> violations = new ArrayList<>();

        // Collect all known IDs (requirements + corner cases)
        Set<String> knownIds = new HashSet<>();
        Map<String, RequirementDefinition> reqById = new HashMap<>();
        Map<String, RequirementDefinition> ccToReq = new HashMap<>();

        for (RequirementDefinition req : contract.getRequirements()) {
            knownIds.add(req.getId());
            reqById.put(req.getId(), req);
            for (CornerCase cc : req.getCornerCases()) {
                knownIds.add(cc.getId());
                ccToReq.put(cc.getId(), req);
            }
        }

        // Collect all referenced IDs from traces
        Set<String> referencedIds = new HashSet<>();
        for (TraceEntry trace : traces) {
            referencedIds.addAll(trace.getRequirementIds());
        }

        // Build map: id -> list of test labels
        Map<String, List<String>> idToTests = new HashMap<>();
        for (TraceEntry trace : traces) {
            String testLabel = trace.getClassName() + "#" + trace.getMethodName();
            for (String id : trace.getRequirementIds()) {
                idToTests.computeIfAbsent(id, k -> new ArrayList<>()).add(testLabel);
            }
        }

        // Rule 1: ORPHANED_ANNOTATION — ID in annotation not found in YAML
        for (String refId : referencedIds) {
            if (!knownIds.contains(refId)) {
                violations.add(new Violation(
                        ViolationType.ORPHANED_ANNOTATION,
                        refId, null,
                        "Annotation references ID '" + refId + "' which is not defined in CONTRACT.yaml"));
            }
        }

        // Rule 2 & 3: UNCOVERED_REQUIREMENT / UNCOVERED_CORNER_CASE for approved requirements
        for (RequirementDefinition req : contract.getRequirements()) {
            if (req.getStatus() != RequirementStatus.APPROVED) continue;

            if (!idToTests.containsKey(req.getId())) {
                violations.add(new Violation(
                        ViolationType.UNCOVERED_REQUIREMENT,
                        req.getId(), null,
                        "Approved requirement '" + req.getId() + "' has no mapped tests"));
            }

            for (CornerCase cc : req.getCornerCases()) {
                if (!idToTests.containsKey(cc.getId())) {
                    if (cc.getTestEnvironment() != null) {
                        violations.add(new Violation(
                                ViolationType.UNCOVERED_CORNER_CASE_ENVIRONMENT,
                                req.getId(), cc.getId(),
                                "Corner case '" + cc.getId() + "' has no mapped test"
                                        + " (requires " + cc.getTestEnvironment().toYaml() + " environment)"));
                    } else {
                        violations.add(new Violation(
                                ViolationType.UNCOVERED_CORNER_CASE,
                                req.getId(), cc.getId(),
                                "Corner case '" + cc.getId() + "' has no mapped test"));
                    }
                }
            }
        }

        // Rule 4: DEPRECATED_REFERENCE — deprecated requirement still referenced
        for (String refId : referencedIds) {
            RequirementDefinition req = reqById.get(refId);
            if (req != null && req.getStatus() == RequirementStatus.DEPRECATED) {
                violations.add(new Violation(
                        ViolationType.DEPRECATED_REFERENCE,
                        refId, null,
                        "Annotation references deprecated requirement '" + refId + "'"));
            }
        }

        // Rule 5: SUPERSEDED_REFERENCE — superseded requirement still referenced
        for (String refId : referencedIds) {
            RequirementDefinition req = reqById.get(refId);
            if (req != null && req.getStatus() == RequirementStatus.SUPERSEDED) {
                violations.add(new Violation(
                        ViolationType.SUPERSEDED_REFERENCE,
                        refId, null,
                        "Annotation references superseded requirement '" + refId
                                + "' (superseded by " + req.getSupersededBy() + ")"));
            }
        }

        return violations;
    }
}
