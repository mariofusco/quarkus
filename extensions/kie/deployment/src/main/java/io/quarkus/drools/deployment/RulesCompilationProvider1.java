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
import org.kie.submarine.codegen.rules.RuleCodegen;

import io.quarkus.dev.JavaCompilationProvider;
import io.quarkus.gizmo.ClassCreator;

public class RulesCompilationProvider1 extends JavaCompilationProvider {

    public String handledExtension() {
        return ".drl";
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
            //            touchEmptyClassFiles(filesToCompile);
        } catch (IOException e) {
            throw new KieCompilerException(e);
        }
    }

    protected Generator addGenerator(ApplicationGenerator appGen, Set<File> filesToCompile, Context context)
            throws IOException {
        return appGen.withGenerator(
                RuleCodegen.ofFiles(
                        context.getOutputDirectory().toPath().getParent().getParent(),
                        filesToCompile));

    }

    /*
     *
     * quarkus core expects compiled(foo/bar/baz/origin.ext) == foo/bar/baz/origin.class
     * so we create one to make it happy
     *
     */
    private void touchEmptyClassFiles(Set<File> filesToCompile) {
        String SRCMAINJAVA = "src/main/java";

        for (File file : filesToCompile) {
            file.getParentFile().mkdirs();
            String filePath = file.getPath();
            int index = filePath.indexOf(SRCMAINJAVA) + SRCMAINJAVA.length() + 1;
            String className = filePath.substring(index)
                    .replace(handledExtension(), "");

            String classFileName = filePath.replace(SRCMAINJAVA, "target/classes")
                    .substring(0, 1 + filePath.length() - handledExtension().length()) + ".class";

            writeEmptyClassFile(className, classFileName);
        }
    }

    private void writeEmptyClassFile(String className, String classFileName) {
        ClassCreator
                .builder()
                .className(className)
                .classOutput((name, data) -> {
                    try {
                        Files.write(Paths.get(classFileName), data);
                    } catch (IOException e) {
                        throw new KieCompilerException(e);
                    }
                }).build().close();
    }

    private Path pathOf(String path, String relativePath) {
        Path p = Paths.get(path, relativePath);
        p.getParent().toFile().mkdirs();
        return p;
    }
}
