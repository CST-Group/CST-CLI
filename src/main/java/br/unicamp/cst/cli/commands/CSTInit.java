package br.unicamp.cst.cli.commands;

import br.unicamp.cst.cli.data.AgentConfig;
import br.unicamp.cst.cli.data.CodeletConfig;
import br.unicamp.cst.cli.data.ConfigParser;
import br.unicamp.cst.cli.util.TemplatesBundle;

import com.github.javaparser.ParseProblemException;
import org.yaml.snakeyaml.error.YAMLException;
import picocli.CommandLine;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Help.Ansi;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Command(name = "init", description = "Initialize a new CST project")
public class CSTInit implements Callable<Integer> {
    public static String TAB = "    ";

    @Option(names = {"--project-name"}, description = "Name of the project")
    String projectName;

    @Option(names = {"--package"}, description = "Package name for the project")
    String packageName;

    @Option(names = {"-f", "--file"}, description = "Config file for project creation")
    File config;

    @Option(names = {"--cst-version"}, defaultValue = "1.4.1", description = "Select a CST release version to use")
    String cstVersion;

    @Option(names = {"--overwrite"}, negatable = true, description = "Allows to overwrite files in the directory", defaultValue = "false")
    Boolean overwrite;

    @Option(names = {"-d", "--dir"}, description = "Root directory for project initialization")
    Path rootFolder;

    @Spec
    CommandSpec spec;

    private Scanner input;

    private AgentConfig agentConfig;
    private AgentConfig currAgentConfig;

    @Override
    public Integer call() {
        try {
            input = new Scanner(System.in);
            checkCurrDir();
            getAgentConfig();
            getRequiredParams();
            createDirs();
            initGradle();
            generateCode();
            return 0;
        } catch (YAMLException e) {
            System.out.println("Configuration File contains errors. Could not parse configurations.");
            String[] errorLines = e.toString().split("\n");
            String errorLine = errorLines[1] + "\n" + errorLines[2] + "\n" + errorLines[3];
            System.out.println(errorLine);
            return 1;
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
            return 1;
        }
    }

    private void checkCurrDir() {
        if (rootFolder == null) {
            rootFolder = Path.of(System.getProperty("user.dir"));
        }
        File[] existingFiles = new File(rootFolder.toUri()).listFiles();
        if (!(existingFiles.length == 0) && !Arrays.stream(existingFiles).allMatch(e -> e.toString().contains(".yaml"))) {
            CommandLine.Model.OptionSpec overwriteOpt = spec.findOption("--overwrite");
            if (!spec.commandLine().getParseResult().hasMatchedOption(overwriteOpt)) {
                String warning = Ansi.AUTO.string("@|bold,red WARNING:|@ @|red This directory is not empty.|@\n"
                        + "Options to resolve conflict are:\n"
                        + "   (1) Overwrite all files\n"
                        + "   (2) Add only different files\n"
                        + "Enter selection (default: 2) ");
                System.out.print(warning);
                String inputName = input.nextLine();
                String ans = "2";
                if (!inputName.isBlank())
                    ans = inputName;
                overwrite = ans.equals("1");
            }
        } else {
            overwrite = true;
        }
    }

    private void getRequiredParams() {
        currAgentConfig = ConfigParser.parseProjectToConfig();
        if (!overwrite) {
            projectName = currAgentConfig.getProjectName();
            packageName = currAgentConfig.getPackageName();
        }
        if (projectName == null) {
            if (agentConfig.getProjectName() == null) {
                String osName = System.getProperty("os.name").toLowerCase();
                String[] splitPath = new String[0];
                if (osName.contains("win"))
                    splitPath = rootFolder.toString().split("\\");
                if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix"))
                    splitPath = rootFolder.toString().split("/");

                projectName = splitPath[splitPath.length - 1];
                System.out.print("Enter project name (default: " + projectName + ") : ");
                String inputName = input.nextLine();
                if (!inputName.isBlank())
                    projectName = inputName;
            } else {
                projectName = agentConfig.getProjectName();
            }
        }
        agentConfig.setProjectName(projectName);

        if (packageName == null) {
            if (agentConfig.getPackageName() == null) {
                packageName = projectName.toLowerCase();
                System.out.print("Enter package name (default: " + packageName + "): ");
                String inputName = input.nextLine();
                if (!inputName.isBlank())
                    packageName = inputName;
            } else {
                packageName = agentConfig.getPackageName();
            }
        }
        agentConfig.setPackageName(packageName);
    }

    private void createDirs() throws IOException {
        // Main java package dir
        File path = new File(rootFolder + "/src/main/java/" + packageName.replace(".", "/") + "/codelets");
        path.mkdirs();
        // Resources dir
        path = new File(rootFolder + "/src/main/resources");
        path.mkdirs();
        // Test package dir
        path = new File(rootFolder + "/src/test/java");
        path.mkdirs();
        // Main.java
        path = new File(rootFolder + "/src/main/java/" + packageName.replace(".", "/"));
        path.mkdirs();
        String mainTemplate = TemplatesBundle.getInstance().getTemplate("MainTemplate");
        mainTemplate = mainTemplate.replace("{{rootPackage}}", packageName);
        FileWriter writer = new FileWriter(path + "/Main.java");
        writer.write(mainTemplate);
        writer.close();

    }

    private void initGradle() throws IOException {
        // Gradle wrappers
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            InputStream gradle = CSTInit.class.getResourceAsStream("/gradle/gradlew");
            OutputStream streamOut = new FileOutputStream(rootFolder + "/gradlew");
            byte[] buffer = new byte[4096];
            int bytes;
            while ((bytes = gradle.read(buffer)) > 0) {
                streamOut.write(buffer, 0, bytes);
            }
            gradle.close();
            streamOut.close();
            // TODO: test if system type
            Runtime.getRuntime().exec("chmod u+x " + rootFolder + "/gradlew");
        } else {
            InputStream gradle = CSTInit.class.getResourceAsStream("/gradle/gradlew.bat");
            OutputStream streamOut = new FileOutputStream(rootFolder + "/gradlew.bat");
            byte[] buffer = new byte[4096];
            int bytes;
            while ((bytes = gradle.read(buffer)) > 0) {
                streamOut.write(buffer, 0, bytes);
            }
            gradle.close();
            streamOut.close();
        }

        // Gradle Wrapper
        File path = new File(rootFolder + "/gradle/wrapper");
        path.mkdirs();

        InputStream gradleJar = CSTInit.class.getResourceAsStream("/gradle/gradle/wrapper/gradle-wrapper.jar");
        OutputStream streamOut = new FileOutputStream(rootFolder + "/gradle/wrapper/gradle-wrapper.jar");
        byte[] buffer = new byte[4096];
        int bytes;
        while ((bytes = gradleJar.read(buffer)) > 0) {
            streamOut.write(buffer, 0, bytes);
        }

        InputStream gradleProperties = CSTInit.class.getResourceAsStream("/gradle/gradle/wrapper/gradle-wrapper.properties");
        streamOut = new FileOutputStream(rootFolder + "/gradle/wrapper/gradle-wrapper.properties");
        buffer = new byte[4096];
        while ((bytes = gradleProperties.read(buffer)) > 0) {
            streamOut.write(buffer, 0, bytes);
        }

        // settings
        String settingsTemplate = TemplatesBundle.getInstance().getTemplate("settings");
        settingsTemplate = settingsTemplate.replace("{{projectName}}", projectName);
        File settingsGradle = new File(rootFolder + "/settings.gradle");
        if (overwrite || !settingsGradle.exists()) {
            FileWriter writer = new FileWriter(rootFolder + "/settings.gradle");
            writer.write(settingsTemplate);
            writer.close();
        }

        // build
        String buildTemplate = TemplatesBundle.getInstance().getTemplate("build");
        buildTemplate = buildTemplate.replace("{{cstVersion}}", cstVersion);
        buildTemplate = buildTemplate.replace("{{mainClass}}", packageName + ".Main");
        File buildGradle = new File(rootFolder + "/build.gradle");
        if (overwrite || !buildGradle.exists()) {
            FileWriter writer = new FileWriter(rootFolder + "/build.gradle");
            writer.write(buildTemplate);
            writer.close();
        }
    }

    private void generateCode() throws IOException {
        for (CodeletConfig codelet : agentConfig.getCodelets()) {
            boolean codeletCodeExists = currAgentConfig.getCodelets().stream()
                    .map(CodeletConfig::getName)
                    .anyMatch(e -> e.equals(codelet.getName()));
            if (overwrite || !codeletCodeExists) {
                File path = new File(rootFolder + "/src/main/java/" + packageName.replace(".", "/") + "/codelets/" + codelet.getGroup().toLowerCase());
                path.mkdirs();
                String codeletCode = "";
                try {
                    codeletCode = codelet.generateCode(packageName);
                } catch (ParseProblemException e) {
                    //TODO: Handle this excpetion
                    throw new IOException();
                }
                FileWriter writer = new FileWriter(path + "/" + codelet.getName() + ".java");
                writer.write(codeletCode);
                writer.close();
            }
        }

        File path = new File(rootFolder + "/src/main/java/" + packageName.replace(".", "/"));
        path.mkdirs();
        if (!overwrite && currAgentConfig.getPackageName() != null)
            agentConfig = currAgentConfig.mergeWith(agentConfig);
        String agentMindCode = agentConfig.generateCode();
        FileWriter writer = new FileWriter(path + "/AgentMind.java");
        writer.write(agentMindCode);
        writer.close();
    }

    private void getAgentConfig() throws IOException {
        String configInfo = "";
        if (config != null) {
            configInfo = Files.lines(config.toPath()).collect(Collectors.joining("\n"));
        }
        if (configInfo.isBlank()) {
            agentConfig = new AgentConfig();
        } else {
            Yaml yamlParser = new Yaml(new Constructor(AgentConfig.class, new LoaderOptions()));
            agentConfig = yamlParser.load(configInfo);
        }
    }

}
