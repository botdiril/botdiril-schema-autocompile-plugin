package com.botdiril.framework.util;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class AutoCompilePlugin implements Plugin<Project>
{
    @Override
    public void apply(Project project)
    {
        var task = project.getTasks()
                          .register("buildSqlSchemas", BuildSchemasTask.class);

        task.configure(t -> t.dependsOn("init"));
    }
}
