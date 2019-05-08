package io.quarkus.drools.deployment;

public class DecisionTablesCompilationProvider extends RulesCompilationProvider {

    @Override
    public String handledExtension() {
        return ".xls";
    }
}
