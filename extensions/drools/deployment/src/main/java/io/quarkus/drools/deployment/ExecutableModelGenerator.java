/*
 * Copyright 2005 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.drools.deployment;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ArchiveRootBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import org.drools.compiler.commons.jci.compilers.CompilationResult;
import org.drools.compiler.compiler.io.memory.MemoryFileSystem;
import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.drools.compiler.kie.builder.impl.KieBuilderImpl;
import org.drools.compiler.kie.builder.impl.MemoryKieModule;
import org.drools.compiler.rule.builder.dialect.java.JavaDialectConfiguration;
import org.drools.modelcompiler.CanonicalKieModule;
import org.drools.modelcompiler.ExecutableModelCodeGenerationProject;
import org.drools.modelcompiler.builder.JavaParserCompiler;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;

import static java.util.stream.Collectors.toList;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

public class ExecutableModelGenerator {

    private static final String PRODUCER_CLASSNAME = "io.quarkus.drools.runtime.RuntimeProducer";

    private static final String PRODUCER_SOURCE = "package io.quarkus.drools.runtime;\n" +
            "\n" +
            "import org.drools.modelcompiler.KieRuntimeBuilder;\n" +
            "\n" +
            "@javax.inject.Singleton\n" +
            "public class RuntimeProducer {\n" +
            "    public static final KieRuntimeBuilder INSTANCE = new org.drools.project.model.ProjectRuntime();\n" +
            "\n" +
            "    @javax.enterprise.inject.Produces\n" +
            "    public KieRuntimeBuilder produce() {\n" +
            "        return INSTANCE;        \n" +
            "    }\n" +
            "}";

    @BuildStep(providesCapabilities = "io.quarkus.drools")
    @Record(STATIC_INIT)
    public void generateModel(ArchiveRootBuildItem root,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<GeneratedClassBuildItem> generatedClasses) {

        MemoryFileSystem srcMfs = new MemoryFileSystem();
        String[] sources = generateSources(root, srcMfs);

        srcMfs.write(toRuntimeSource(PRODUCER_CLASSNAME), PRODUCER_SOURCE.getBytes());
        sources[sources.length - 1] = toRuntimeSource(PRODUCER_CLASSNAME);

        registerGeneratedClasses(generatedBeans, generatedClasses, compileSources(srcMfs, sources));
    }

    private void registerGeneratedClasses(BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<GeneratedClassBuildItem> generatedClasses, MemoryFileSystem trgMfs) {
        for (String fileName : trgMfs.getFileNames()) {
            byte[] bytes = trgMfs.getBytes(fileName);
            String className = toClassName(fileName);
            if (className.equals(PRODUCER_CLASSNAME)) {
                generatedBeans.produce(new GeneratedBeanBuildItem(className, bytes));
            } else {
                generatedClasses.produce(new GeneratedClassBuildItem(true, className, bytes));
            }
        }
    }

    private MemoryFileSystem compileSources(MemoryFileSystem srcMfs, String[] sources) {
        MemoryFileSystem trgMfs = new MemoryFileSystem();
        CompilationResult res = JavaParserCompiler.getCompiler(JavaDialectConfiguration.CompilerType.ECLIPSE)
                .compile(sources, srcMfs, trgMfs, getClassLoader());

        if (res.getErrors().length != 0) {
            Arrays.stream(res.getErrors()).forEach(System.out::println);
            throw new RuntimeException("Compilation failure!");
        }
        return trgMfs;
    }

    private String[] generateSources(ArchiveRootBuildItem root, MemoryFileSystem srcMfs) {
        MemoryFileSystem model = generateModel(root);
        List<String> sourceNames = model.getFileNames().stream().filter(name -> name.endsWith(".java")).collect(toList());
        String[] sources = new String[sourceNames.size() + 1];
        for (int i = 0; i < sourceNames.size(); i++) {
            String sourceName = sourceNames.get(i);
            sources[i] = toRuntimeSource(toClassName(sourceName));
            srcMfs.write(sources[i], model.getBytes(sourceName));
        }
        return sources;
    }

    private MemoryFileSystem generateModel(ArchiveRootBuildItem root) {
        Path targetClassesPath = root.getPath();
        Path projectPath = targetClassesPath.getParent().getParent();

        KieBuilder kieBuilder = KieServices.get().newKieBuilder(projectPath.toFile(), getClassLoader());
        ((KieBuilderImpl) kieBuilder).buildAll(ExecutableModelCodeGenerationProject.class, o -> false);

        InternalKieModule kieModule = (InternalKieModule) kieBuilder.getKieModule();
        return getMemoryFileSystem(kieModule);
    }

    private MemoryFileSystem getMemoryFileSystem(InternalKieModule kieModule) {
        return kieModule instanceof CanonicalKieModule
                ? ((MemoryKieModule) ((CanonicalKieModule) kieModule).getInternalKieModule()).getMemoryFileSystem()
                : ((MemoryKieModule) kieModule).getMemoryFileSystem();
    }

    private ClassLoader getClassLoader() {
        return this.getClass().getClassLoader();
    }

    private String toRuntimeSource(String className) {
        return "src/main/java/" + className.replace('.', '/') + ".java";
    }

    private String toClassName(String sourceName) {
        if (sourceName.startsWith("./")) {
            sourceName = sourceName.substring(2);
        }
        if (sourceName.endsWith(".java")) {
            sourceName = sourceName.substring(0, sourceName.length() - 5);
        } else if (sourceName.endsWith(".class")) {
            sourceName = sourceName.substring(0, sourceName.length() - 6);
        }
        return sourceName.replace('/', '.');
    }
}
