package com.matador;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Verifies the Spring Modulith structure: no module reaches into another module's
 * {@code internal} package and there are no cyclic dependencies. Also writes module
 * documentation under target/build output.
 */
class ModularityTests {

    private static final ApplicationModules modules = ApplicationModules.of(MatadorApplication.class);

    @Test
    void verifiesModuleStructure() {
        modules.verify();
    }

    @Test
    void writesDocumentation() {
        new Documenter(modules).writeModulesAsPlantUml().writeIndividualModulesAsPlantUml();
    }
}
