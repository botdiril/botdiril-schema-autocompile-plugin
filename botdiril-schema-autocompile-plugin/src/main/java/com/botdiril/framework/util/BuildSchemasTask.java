package com.botdiril.framework.util;

import org.apache.commons.lang3.function.Failable;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetOutput;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


public class BuildSchemasTask extends DefaultTask
{
    private final Path outputDir;

    private final Set<SourceSet> sourceSets;

    public BuildSchemasTask()
    {
        this.setGroup("build");

        var project = this.getProject();
        this.outputDir = project.getBuildDir()
                                .toPath()
                                .resolve("generated/botdiril-sql");

        project.getConfigurations().create("buildSqlSchemas", ca -> {

        });

        this.sourceSets = new HashSet<>();
    }

    public void addSourceSet(SourceSet sourceSet)
    {
        this.sourceSets.add(sourceSet);

        this.dependsOn(sourceSet.getCompileJavaTaskName());
    }

    private static Set<Path> getClassesInTree(Path dir) throws IOException
    {
        try (var tree = Files.walk(dir))
        {
            return tree.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".class"))
                       .map(dir::relativize)
                       .collect(Collectors.toSet());
        }
    }

    @TaskAction
    public void buildSqlSchemas()
    {
        try
        {
            if (Files.isDirectory(this.outputDir))
            {
                try (var files = Files.walk(this.outputDir))
                {
                    files.sorted(Comparator.reverseOrder())
                         .forEach(Failable.asConsumer(Files::delete));
                }
            }

            Files.createDirectories(this.outputDir);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }


        if (this.sourceSets.isEmpty())
            return;

        var classFiles = this.sourceSets.stream()
                                        .map(SourceSet::getOutput)
                                        .map(SourceSetOutput::getClassesDirs)
                                        .flatMap(so -> so.getFiles().stream())
                                        .map(File::toPath)
                                        .filter(Files::isDirectory)
                                        .map(Failable.asFunction(BuildSchemasTask::getClassesInTree))
                                        .flatMap(Set::stream)
                                        .collect(Collectors.toSet());

        var urlSet = this.sourceSets.stream()
                                    .map(SourceSet::getOutput)
                                    .flatMap(so -> StreamSupport.stream(so.spliterator(), false))
                                    .map(File::toPath)
                                    .filter(Files::isDirectory)
                                    .map(Path::toUri)
                                    .map(Failable.asFunction(URI::toURL))
                                    .collect(Collectors.toCollection(HashSet::new));

        this.getProject()
            .getConfigurations()
            .getByName("runtimeClasspath")
            .getAsFileTree()
            .getFiles()
            .stream()
            .map(File::toURI)
            .map(Failable.asFunction(URI::toURL))
            .forEach(urlSet::add);

        try (var ucl = new URLClassLoader(urlSet.toArray(URL[]::new)))
        {
            var sqlConClass = ucl.loadClass("com.botdiril.framework.sql.connection.SqlConnectionConfig");

            var mmClass = ucl.loadClass("com.botdiril.framework.sql.orm.ModelManager");
            var mmConstructor = mmClass.getConstructor(sqlConClass);
            var mmRegister = mmClass.getMethod("registerModels", ucl.loadClass("java.lang.Class"));
            var mmGetModels = mmClass.getMethod("getModels");

            var modelClass = ucl.loadClass("com.botdiril.framework.sql.orm.Model");

            var tigClass = ucl.loadClass("com.botdiril.framework.sql.orm.asm.TableInfoGenerator");
            var tigGenerate = tigClass.getMethod("generate", modelClass);

            var genClassRecord = ucl.loadClass("com.botdiril.framework.sql.orm.asm.GeneratedClass");
            var dataGetter = genClassRecord.getMethod("data");
            var nameGetter = genClassRecord.getMethod("name");
            var pathGetter = genClassRecord.getMethod("path");

            var autoCloseable = ucl.loadClass("java.lang.AutoCloseable");

            try (var mm = (AutoCloseable) autoCloseable.cast(mmConstructor.newInstance(sqlConClass.cast(null))))
            {
                var sep = FileSystems.getDefault().getSeparator();

                classFiles.stream()
                          .map(Path::toString)
                          .map(strPath -> strPath.replaceAll(Pattern.quote(sep), "."))
                          .map(clzName -> clzName.replaceAll("\\.class$", ""))
                          .map(Failable.asFunction(ucl::loadClass))
                          .forEach(Failable.asConsumer(clazz -> mmRegister.invoke(mm, clazz)));

                var modelMap = (Map<?, ?>) mmGetModels.invoke(mm);

                var classes = modelMap.values()
                                      .stream()
                                      .map(Failable.asFunction(model -> (List<?>) tigGenerate.invoke(null, modelClass.cast(model))))
                                      .flatMap(List::stream)
                                      .toList();

                for (var generatedClass : classes)
                {
                    var pkg = (String) pathGetter.invoke(generatedClass);
                    var packageDir = this.outputDir.resolve(pkg);
                    var cName = (String) nameGetter.invoke(generatedClass);
                    var cfName = cName.substring(cName.lastIndexOf(".") + 1);

                    Files.createDirectories(packageDir);

                    var path = packageDir.resolve(cfName + ".class");

                    Files.write(path, (byte[]) dataGetter.invoke(generatedClass));
                }
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

    }
}
