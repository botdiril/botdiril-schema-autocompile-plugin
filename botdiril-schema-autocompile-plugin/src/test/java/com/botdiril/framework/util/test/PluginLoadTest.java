package com.botdiril.framework.util.test;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PluginLoadTest
{
    @Test
    public void applyTest()
    {
        Project project = ProjectBuilder.builder().build();

        project.task("init");

        project.getPluginManager().apply("com.botdiril.botdiril-schema-autocompile-plugin");

        assertTrue(project.getPluginManager().hasPlugin("com.botdiril.botdiril-schema-autocompile-plugin"));

        assertNotNull(project.getTasks().getByName("buildSqlSchemas"));
    }
}
