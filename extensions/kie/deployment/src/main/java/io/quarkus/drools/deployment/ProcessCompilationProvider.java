package io.quarkus.drools.deployment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.kie.submarine.codegen.ApplicationGenerator;
import org.kie.submarine.codegen.GeneratedFile;
import org.kie.submarine.codegen.Generator;
import org.kie.submarine.codegen.process.ProcessCodegen;
import org.kie.submarine.codegen.rules.RuleCodegen;

import io.quarkus.dev.JavaCompilationProvider;
import io.quarkus.gizmo.ClassCreator;

public class ProcessCompilationProvider extends KieCompilationProvider {

    @Override
    public String handledExtension() {
        return ".bpmn";
    }

    @Override
    protected Generator addGenerator(ApplicationGenerator appGen, Set<File> filesToCompile, Context context)
            throws IOException {
        return appGen.withGenerator(
                ProcessCodegen.ofFiles(new ArrayList<>(filesToCompile)));
    }
}
