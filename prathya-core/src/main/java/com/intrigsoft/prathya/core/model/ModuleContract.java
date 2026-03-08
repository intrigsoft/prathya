package com.intrigsoft.prathya.core.model;

import java.util.ArrayList;
import java.util.List;

public class ModuleContract {

    private ModuleInfo module;
    private List<RequirementDefinition> requirements = new ArrayList<>();

    public ModuleContract() {}

    public ModuleContract(ModuleInfo module, List<RequirementDefinition> requirements) {
        this.module = module;
        this.requirements = requirements;
    }

    public ModuleInfo getModule() { return module; }
    public void setModule(ModuleInfo module) { this.module = module; }

    public List<RequirementDefinition> getRequirements() { return requirements; }
    public void setRequirements(List<RequirementDefinition> requirements) { this.requirements = requirements; }
}
