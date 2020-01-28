/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.gradle;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSetContainer;
import software.amazon.smithy.cli.Cli;
import software.amazon.smithy.cli.SmithyCli;

/**
 * General utility methods used throughout the plugin.
 */
public final class SmithyUtils {

    public static final String SMITHY_PROJECTIONS = "smithyprojections";
    private static final String MAIN_SOURCE_SET = "main";
    private static final String SMITHY_SOURCE_SET_EXTENSION = "smithy";
    private static final String SOURCE_SETS_PROPERTY = "sourceSets";

    private SmithyUtils() {}

    /**
     * Gets the {@code SmithyExtension} extension of a {@code Project}.
     *
     * @param project Project to query.
     * @return Returns the extension.
     */
    public static SmithyExtension getSmithyExtension(Project project) {
        return project.getExtensions().getByType(SmithyExtension.class);
    }

    /**
     * Gets the path to a projection plugins output.
     *
     * @param project Project to inspect.
     * @param projection Projection name.
     * @param plugin Plugin name.
     * @return Returns the resolved path.
     */
    public static Path getProjectionPluginPath(Project project, String projection, String plugin) {
        return project.getBuildDir().toPath()
                .resolve(SMITHY_PROJECTIONS)
                .resolve(project.getName())
                .resolve(projection)
                .resolve(plugin);
    }

    /**
     * Gets the source sets of a project.
     *
     * @param project Project to inspect.
     * @return Returns the project's source sets.
     */
    public static SourceSetContainer getSourceSets(Project project) {
        return (SourceSetContainer) project.getProperties().get(SOURCE_SETS_PROPERTY);
    }

    /**
     * Gets the Smithy model sources of the "main" source set.
     *
     * @param project Project to inspect.
     * @return Returns the Smithy model sources.
     */
    public static SourceDirectorySet getSmithyModelSources(Project project) {
        return getSmithySourceDirectory(project, MAIN_SOURCE_SET);
    }

    private static SourceDirectorySet getSmithySourceDirectory(Project project, String name) {
        project.getLogger().info("" + project + " " + name);
        // Grab a list of all the files and directories to mark as sources.
        return (SourceDirectorySet) project.getConvention()
                .getPlugin(JavaPluginConvention.class)
                .getSourceSets()
                .getByName(name)
                .getAllSource();
    }

    /**
     * Gets the classpath used with the "smithyCli" configuration.
     *
     * @param project Project to inspect.
     * @return Returns the Smithy CLI classpath used to run the CLI.
     */
    private static Configuration getSmithyCliClasspath(Project project) {
        return getClasspath(project, "smithyCli");
    }

    /**
     * Gets the classpath of a project by name.
     *
     * @param project Project to inspect.
     * @param configurationName Name of the classpath to retrieve.
     * @return Returns the classpath.
     */
    public static Configuration getClasspath(Project project, String configurationName) {
        return project.getConfigurations().getByName(configurationName);
    }

    /**
     * Gets the buildscript classpath of a project.
     *
     * @param project Project to inspect.
     * @return Returns the classpath.
     */
    public static Configuration getBuildscriptClasspath(Project project) {
        return project.getBuildscript().getConfigurations().getByName("classpath");
    }

    /**
     * Gets the path to the temp directory where Smithy model resources are placed
     * in the generated JAR of a project.
     *
     * @param project Project to inspect.
     * @return Returns the classpath.
     */
    public static File getSmithyResourceTempDir(Project project) {
        return project.getBuildDir()
                .toPath()
                .resolve("tmp")
                .resolve("smithy-inf")
                .resolve("META-INF")
                .resolve("smithy")
                .toFile();
    }

    /**
     * Gets the path to the default output directory of projections.
     *
     * @param project Project to inspect.
     * @return Returns the default output directory.
     */
    private static File getProjectionOutputDir(Project project) {
        return project.getProjectDir()
                .toPath()
                .resolve("build")
                .resolve(SMITHY_PROJECTIONS)
                .resolve(project.getName())
                .toFile();
    }

    /**
     * Resolves the appropriate output directory for Smithy artifacts.
     *
     * @param currentTaskValue The possibly null value currently set on a task.
     * @param project The project to query.
     * @return Returns the resolved directory.
     */
    public static File resolveOutputDirectory(File currentTaskValue, Project project) {
        if (currentTaskValue != null) {
            return currentTaskValue;
        }

        SmithyExtension extension = getSmithyExtension(project);
        return extension.getOutputDirectory() != null
               ? extension.getOutputDirectory()
               : SmithyUtils.getProjectionOutputDir(project);
    }

    /**
     * Executes the Smithy CLI in a separate thread or process.
     *
     * @param project Gradle project being built.
     * @param arguments CLI arguments.
     * @param classpath Classpath to use when running the CLI. Uses buildScript when not defined.
     */
    public static void executeCli(Project project, List<String> arguments, FileCollection classpath) {
        FileCollection resolvedClasspath = resolveCliClasspath(project, classpath);
        boolean fork = getSmithyExtension(project).getFork();
        project.getLogger().info("Executing Smithy CLI in a {}: {}; using classpath {}",
                                 fork ? "process" : "thread",
                                 String.join(" ", arguments),
                                 resolvedClasspath.getAsPath());
        if (fork) {
            executeCliProcess(project, arguments, resolvedClasspath);
        } else {
            executeCliThread(project, arguments, resolvedClasspath);
        }
    }

    private static FileCollection resolveCliClasspath(Project project, FileCollection cliClasspath) {
        if (cliClasspath == null) {
            FileCollection buildScriptCp = SmithyUtils.getBuildscriptClasspath(project);
            project.getLogger().info("Smithy CLI classpath is null, so using buildscript: {}", buildScriptCp);
            return resolveCliClasspath(project, buildScriptCp);
        }

        // Add the CLI classpath if it's missing from the given classpath.
        if (!cliClasspath.getAsPath().contains("smithy-cli")) {
            FileCollection defaultCliCp = SmithyUtils.getSmithyCliClasspath(project);
            project.getLogger().info("Adding CLI classpath to command: {}", defaultCliCp.getAsPath());
            cliClasspath = cliClasspath.plus(defaultCliCp);
        } else {
            project.getLogger().info("Smithy CLI classpath already has the CLI in it");
        }

        for (File f : cliClasspath) {
            if (!f.exists()) {
                project.getLogger().error("CLI classpath JAR does not exist: {}", f);
            }
        }

        return cliClasspath;
    }

    private static void executeCliProcess(Project project, List<String> arguments, FileCollection classpath) {
        project.javaexec(t -> {
            t.setArgs(arguments);
            t.setClasspath(classpath);
            t.setMain(SmithyCli.class.getCanonicalName());
        });
    }

    @SuppressWarnings("unchecked")
    private static void executeCliThread(Project project, List<String> arguments, FileCollection classpath) {
        // Create a custom class loader to run within the context of.
        Set<File> files = classpath.getFiles();
        URL[] paths = new URL[files.size()];
        int i = 0;

        for (File file : files) {
            try {
                paths[i++] = file.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        Logger logger = project.getLogger();

        // Need to run this in a doPrivileged to pass SpotBugs.
        try (URLClassLoader classLoader = AccessController.doPrivileged(
                (PrivilegedExceptionAction<URLClassLoader>) () -> new URLClassLoader(paths))) {

            // Reflection is used to make calls on the loaded SmithyCli object.
            String smithyCliName = SmithyCli.class.getCanonicalName();
            String cliName = Cli.class.getCanonicalName();

            Thread thread = new Thread(() -> {
                try {
                    Class cliClass = classLoader.loadClass(cliName);
                    Class smithyCliClass = classLoader.loadClass(smithyCliName);
                    Object cli = smithyCliClass.getDeclaredMethod("create").invoke(null);
                    smithyCliClass.getDeclaredMethod("classLoader", ClassLoader.class).invoke(cli, classLoader);
                    overrideCliStdout(cliClass, logger);
                    smithyCliClass.getDeclaredMethod("run", List.class).invoke(cli, arguments);
                } catch (ReflectiveOperationException e) {
                    logger.info("Error executing Smithy CLI (ReflectiveOperationException)", e);
                    throw new RuntimeException(e);
                }
            });

            // Configure the thread to re-throw exception and use our custom class loader.
            thread.setContextClassLoader(classLoader);
            ExceptionHandler handler = new ExceptionHandler();
            thread.setUncaughtExceptionHandler(handler);
            thread.start();
            thread.join();

            if (handler.e != null) {
                logger.info("Error executing Smithy CLI (thread handler)", handler.e);
                throw handler.e;
            }

        } catch (Throwable e) {
            // Find the originating exception message.
            String message;
            Throwable current = e;
            do {
                message = current.getMessage();
                current = current.getCause();
            } while (current != null);
            logger.error(message);
            throw new GradleException(message, e);
        }
    }

    // ** This is a hack! **
    //
    // Gradle attempts to intercept writing to System.out and System.err by
    // sending each write to Gradle's logging system. However, when paired with
    // org.gradle.parallel=true, Gradle sometimes writes stdout and sometimes
    // it doesn't. This implies that their logging implementation and the way
    // they intercept stderr and stdout is not thread-safe. Smithy's build
    // process is run inside of a thread so that we can use the same JVM process
    // but completely change the thread's ClassLoader using the appropriate
    // dependencies (e.g., runtime vs buildscript). Smithy then further
    // utilized thread pools when building projections and plugins. Somewhere
    // along the way, thread-safety is lost and the Smithy CLI's writing to
    // stdout is lost.
    //
    // This hack required that a new method was introduced to the Smithy CLI
    // that uses a Consumer<String> to write a line of text. By default, the
    // CLI writes to System.out.println and System.err.println, but this change
    // cause the Smithy CLI to write warning messages to the provided Logger.
    // Why warning? Changing the logging level of a task also appears to be
    // unreliable. Maybe there's a way to do it so that we can log using info
    // and then change the log level to info, but I couldn't find it.
    @SuppressWarnings("unchecked")
    private static void overrideCliStdout(Class cliClass, Logger logger) {
        try {
            cliClass.getDeclaredMethod("setStdout", Consumer.class).invoke(null, new Consumer<String>() {
                @Override
                public void accept(String s) {
                    logger.warn(s);
                }
            });
        } catch (ReflectiveOperationException e) {
            logger.warn("Found an old version of Smithy CLI that does not support Cli#setStdout");
        }
    }

    private static final class ExceptionHandler implements Thread.UncaughtExceptionHandler {
        volatile Throwable e;

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            this.e = e;
        }
    }
}
