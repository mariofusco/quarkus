package io.quarkus.drools.deployment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.kie.submarine.codegen.ApplicationGenerator;
import org.kie.submarine.codegen.GeneratedFile;
import org.kie.submarine.codegen.Generator;

import io.quarkus.dev.JavaCompilationProvider;

public abstract class KieCompilationProvider extends JavaCompilationProvider {

    public abstract String handledExtension();

    @Override
    public boolean isCompiledPathModified(Path resource, Path sourcesDir, Path classesDir, long sourceMod) {
        return false;
    }

    @Override
    public final void compile(Set<File> filesToCompile, Context context) {
        String appPackageName = System.getProperty("kie.codegen.packageName", "org.kie");
        File outputDirectory = context.getOutputDirectory();
        try {

            ApplicationGenerator appGen = new ApplicationGenerator(appPackageName, outputDirectory)
                    .withDependencyInjection(true);
            Generator generator = addGenerator(appGen, filesToCompile, context);

            Collection<GeneratedFile> generatedFiles = generator.generate();

            HashSet<File> generatedSourceFiles = new HashSet<>();
            for (GeneratedFile file : generatedFiles) {
                Path path = pathOf(outputDirectory.getPath(), file.relativePath());
                Files.write(path, file.contents());
                generatedSourceFiles.add(path.toFile());
            }
            super.compile(generatedSourceFiles, context);
        } catch (IOException e) {
            throw new KieCompilerException(e);
        }
    }

    protected abstract Generator addGenerator(ApplicationGenerator appGen, Set<File> filesToCompile, Context context)
            throws IOException;

    private Path pathOf(String path, String relativePath) {
        Path p = Paths.get(path, relativePath);
        p.getParent().toFile().mkdirs();
        return p;
    }
}
