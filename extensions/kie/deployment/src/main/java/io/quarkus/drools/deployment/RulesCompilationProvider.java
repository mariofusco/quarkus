package io.quarkus.drools.deployment;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.kie.api.io.ResourceType;
import org.kie.submarine.codegen.ApplicationGenerator;
import org.kie.submarine.codegen.Generator;
import org.kie.submarine.codegen.rules.IncrementalRuleCodegen;

public class RulesCompilationProvider extends KieCompilationProvider {

    @Override
    public String handledExtension() {
        return ".drl";
    }

    @Override
    protected Generator addGenerator(ApplicationGenerator appGen, Set<File> filesToCompile, Context context)
            throws IOException {
        return new IncrementalRuleCodegen(context.getOutputDirectory().toPath().getParent().getParent(), filesToCompile,
                ResourceType.DRL);
        //        return appGen.withGenerator(
        //                RuleCodegen.ofFiles(
        //                        context.getOutputDirectory().toPath().getParent().getParent(),
        //                        filesToCompile));
    }
}
