package io.pactum.core;

import io.pactum.annotations.Pact;
import io.pactum.annotations.Requirement;
import io.pactum.annotations.Requirements;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * The core pactum verification engine.
 * Scans classes for {@link Pact} and {@link Requirement} annotations and reports
 * verification results.
 */
public class PactumEngine {

    /**
     * Verifies all pacts declared on the given classes.
     *
     * @param classes the classes to inspect
     * @return a {@link VerificationResult} summarising the outcome
     */
    public VerificationResult verify(Class<?>... classes) {
        List<String> passed = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        for (Class<?> clazz : classes) {
            inspectClass(clazz, passed, failed);
        }

        return new VerificationResult(passed, failed);
    }

    private void inspectClass(Class<?> clazz, List<String> passed, List<String> failed) {
        Pact classPact = clazz.getAnnotation(Pact.class);
        if (classPact != null) {
            String label = buildLabel(clazz.getSimpleName(), classPact);
            passed.add(label);
        }

        for (Method method : clazz.getDeclaredMethods()) {
            Pact methodPact = method.getAnnotation(Pact.class);
            if (methodPact != null) {
                String label = buildLabel(clazz.getSimpleName() + "#" + method.getName(), methodPact);
                passed.add(label);
            }

            Requirements requirements = method.getAnnotation(Requirements.class);
            if (requirements != null) {
                for (Requirement req : requirements.value()) {
                    passed.add("Requirement[" + req.id() + "] covered by " + clazz.getSimpleName() + "#" + method.getName());
                }
            }

            Requirement singleReq = method.getAnnotation(Requirement.class);
            if (singleReq != null) {
                passed.add("Requirement[" + singleReq.id() + "] covered by " + clazz.getSimpleName() + "#" + method.getName());
            }
        }
    }

    private String buildLabel(String target, Pact pact) {
        StringBuilder sb = new StringBuilder("Pact[");
        sb.append(target).append("]");
        if (!pact.consumer().isEmpty()) {
            sb.append(" consumer=").append(pact.consumer());
        }
        if (!pact.provider().isEmpty()) {
            sb.append(" provider=").append(pact.provider());
        }
        return sb.toString();
    }
}
