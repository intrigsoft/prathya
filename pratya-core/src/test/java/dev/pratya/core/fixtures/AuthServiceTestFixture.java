package dev.pratya.core.fixtures;

import dev.pratya.annotations.Requirement;

/**
 * Stub test class used as a fixture for scanner tests.
 * Methods annotated with @Requirement simulate real test classes.
 */
public class AuthServiceTestFixture {

    @Requirement("AUTH-001")
    public void loginWithValidCredentials() {}

    @Requirement({"AUTH-001-CC-001", "AUTH-001-CC-002"})
    public void loginWithUnknownEmail() {}

    @Requirement("AUTH-002")
    public void refreshToken() {}

    @Requirement("AUTH-002-CC-001")
    public void refreshTokenUsedTwice() {}

    public void helperMethodWithoutAnnotation() {}
}
