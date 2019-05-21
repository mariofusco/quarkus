package io.quarkus.drools.deployment;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.kie.submarine.codegen.ApplicationGenerator;
import org.kie.submarine.codegen.Generator;
import org.kie.submarine.codegen.process.ProcessCodegen;

public class ProcessCompilationProvider extends KieCompilationProvider {

    @Override
    public Set<String> handledExtensions() {
        return new HashSet<>(asList(".bpmn", ".bpmn2"));
    }

    @Override
    protected Generator addGenerator(ApplicationGenerator appGen, Set<File> filesToCompile, Context context)
            throws IOException {
        return appGen.withGenerator(
                ProcessCodegen.ofFiles(new ArrayList<>(filesToCompile)));
    }
}
