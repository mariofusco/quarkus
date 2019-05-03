package io.quarkus.drools.deployment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import org.kie.submarine.codegen.ApplicationGenerator;
import org.kie.submarine.codegen.Generator;
import org.kie.submarine.codegen.process.ProcessCodegen;
import org.kie.submarine.codegen.rules.RuleCodegen;

public class RulesCompilationProvider extends KieCompilationProvider {

    @Override
    public String handledExtension() {
        return ".drl";
    }

    @Override
    protected Generator addGenerator(ApplicationGenerator appGen, Set<File> filesToCompile, Context context)
            throws IOException {
        return appGen.withGenerator(
                RuleCodegen.ofFiles(
                        context.getOutputDirectory().toPath().getParent().getParent(),
                        filesToCompile));
    }
}
