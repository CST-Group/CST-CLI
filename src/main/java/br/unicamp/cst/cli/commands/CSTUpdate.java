package br.unicamp.cst.cli.commands;

import br.unicamp.cst.cli.data.AgentConfig;
import br.unicamp.cst.cli.data.CodeletConfig;
import br.unicamp.cst.cli.data.ConfigParser;
import br.unicamp.cst.cli.util.CodeUtils;
import com.github.javaparser.ParseProblemException;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;


@Command(name = "update",
    description = "Updates current project by applying differences found on a new YAML file",
    mixinStandardHelpOptions = true
)
public class CSTUpdate implements Callable<Integer> {

    Path rootFolder;
    AgentConfig agentConfig;
    AgentConfig currAgentConfig = ConfigParser.parseProjectToConfig();

    @CommandLine.Option(names = {"-f", "--file"}, description = "Config file for project creation")
    File config;

    @CommandLine.Option(names = {"--editor", "-e"}, description = "Open current config into default editor for modification")
    boolean openEditor;

    @Override
    public Integer call(){
        if (findRootFolder()){
            if (getAgentConfig()){
                try {
                    generateCode();
                } catch (IOException e) {
                    //TODO: Need to handle this??
                    return 1;
                }
                return 0;
            }
        }
        return 1;
    }

    private boolean findRootFolder(){
        File currDir = new File(System.getProperty("user.dir"));

        File srcFolder = new File(currDir.getAbsolutePath() + "/src");
        while (currDir != null && !srcFolder.exists()) {
            srcFolder = new File(currDir.getAbsolutePath() + "/src");
            currDir = currDir.getParentFile();
        }

        if (!srcFolder.exists()){
            System.out.println(Ansi.AUTO.string("@|bold,red Current folder is not part of a CST project|@"));
            return false;
        } else {
            rootFolder = currDir.toPath();
            return true;
        }
    }

    private boolean getAgentConfig() {
        String configInfo = "";
        if (config != null) {
            try {
                configInfo = Files.lines(config.toPath()).collect(Collectors.joining("\n"));
            } catch (IOException e) {
                System.out.printf(Ansi.AUTO.string("@|bold,red ERROR: |@ Could not read file \"%s\"\n"), config.getName());
                return false;
            }
        }
        if (configInfo.isBlank()) {
            try {
                configInfo = CodeUtils.getConfigStringFromEditor(rootFolder, "UPDATE", currAgentConfig.toYaml());
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            //System.out.println(Ansi.AUTO.string("@|bold,red WARNIG: |@ Config file is empty. No changes applied."));
            //return false;
        }

        Yaml yamlParser = new Yaml(new Constructor(AgentConfig.class, new LoaderOptions()));
        agentConfig = yamlParser.load(configInfo);

        return true;
    }

    private void generateCode() throws IOException {
        String packageName = currAgentConfig.getPackageName();
        for (CodeletConfig codelet : agentConfig.getCodelets()) {
            boolean codeletCodeExists = currAgentConfig.getCodelets().stream()
                    .map(CodeletConfig::getName)
                    .anyMatch(e -> e.equals(codelet.getName()));
            if (!codeletCodeExists) {
                String codeletPath = rootFolder + "/src/main/java/" + packageName.replace(".", "/") + "/codelets";
                if (codelet.getGroup() != null)
                    codeletPath += "/" + codelet.getGroup().toLowerCase();
                File path = new File(codeletPath);
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
            } else {
                //TODO: Merge update
            }
        }

        File path = new File(rootFolder + "/src/main/java/" + packageName.replace(".", "/"));
        path.mkdirs();
        String agentMindCode = agentConfig.generateCode();
        String fullCurrentCode = String.join("\n", Files.readAllLines(path.toPath()));
        agentMindCode = CodeUtils.generateMergeUpdateCodes(currAgentConfig, agentConfig, fullCurrentCode);
        FileWriter writer = new FileWriter(path + "/AgentMind.java");
        writer.write(agentMindCode);
        writer.close();
    }

}
