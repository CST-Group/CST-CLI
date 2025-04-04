package br.unicamp.cst.cli.commands;

import br.unicamp.cst.cli.data.*;
import br.unicamp.cst.cli.util.CodeUtils;
import com.github.javaparser.ParseProblemException;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Help.Ansi;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static br.unicamp.cst.cli.data.MemoryConfig.*;

@Command(name = "add",
        description = "Adds a new codelet to the project structure",
        mixinStandardHelpOptions = true
        )
public class CSTAdd {

    Scanner input = new Scanner(System.in);
    AgentConfig currAgentConfig = ConfigParser.parseProjectToConfig();
    AgentConfig modifiedConfig = ConfigParser.parseProjectToConfig();
    List<CodeletConfig> newCodelets = new ArrayList<>();

    Path rootFolder;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Command(name = "codelet", description = "Add a codelet to current project", mixinStandardHelpOptions = true)
    private int createCodelet(@Option(names = {"--editor", "-e"}) boolean openEditor){
        if (findRootFolder()) {
            processCreateCodelet(openEditor);
            try {
                applyChanges();
            } catch (IOException e) {
                //TODO: Should I handle this exception in applyChanges()??
                return 1;
            }
            return 0;
        }else {
            return 1;
        }
    }

    @Command(name = "memory", description = "Add a memory to current project", mixinStandardHelpOptions = true)
    private int createMemory(@Option(names = {"--editor", "-e"}) boolean openEditor){
        if (findRootFolder()) {
            processCreateMemory(openEditor);
            try {
                applyChanges();
            } catch (IOException e) {
                //TODO: Should I handle this exception in applyChanges()??
                return 1;
            }
            return 0;
        }else {
            return 1;
        }
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

    private void applyChanges() throws IOException {
        for (CodeletConfig codelet : newCodelets) {
            String codeletPath = rootFolder + "/src/main/java/" + currAgentConfig.getPackageName().replace(".", "/") + "/codelets";
            if (codelet.getGroup() != null)
                codeletPath += "/" + codelet.getGroup().toLowerCase();
            File path = new File(codeletPath);
            path.mkdirs();
            String codeletCode = "";
            try {
                codeletCode = codelet.generateCode(currAgentConfig.getPackageName());
            } catch (ParseProblemException e) {
                //TODO: Handle this excpetion
                throw new IOException();
            }
            FileWriter writer = new FileWriter(path + "/" + codelet.getName() + ".java");
            writer.write(codeletCode);
            writer.close();
        }


        File path = new File(rootFolder + "/src/main/java/" + currAgentConfig.getPackageName().replace(".", "/") + "/AgentMind.java");
        //Base version of code to compare with original (may contain comments and auxiliary functions)
        //and with the modified version of project.
        String currAgentCode = "";
        if (path.exists()){
            currAgentCode = String.join("\n", Files.readAllLines(path.toPath()));
        }
        String mergedCode = CodeUtils.mergeCodes(currAgentConfig.generateCode(), modifiedConfig.generateCode(), currAgentCode);

        FileWriter writer = new FileWriter(path);
        writer.write(mergedCode);
        writer.close();
    }

    private void processCreateCodelet(boolean openEditor){
        CodeletConfig newCodelet;
        if (openEditor){
            try {
                String template = """
                        name:             #Codelet name (required)
                        group:            #Codelet group
                        in: []            #Input memories
                        out: []           #Output memories
                        broadcast: []     #Broadcast memories""";
                String newCodeletConfig = getConfigStringFromEditor("CODELET", template);
                Yaml parser = new Yaml(new Constructor(CodeletConfig.class, new LoaderOptions()));
                newCodelet = parser.load(newCodeletConfig);

            } catch (IOException | InterruptedException e) {
                //TODO
                throw new RuntimeException(e);
            }
        } else {
            //Ask codelet name
            System.out.print("Codelet Name: ");
            String codeletName = input.nextLine();
            while (codeletName.isBlank()) {
                System.out.println(Ansi.AUTO.string("@|red Codelet name cannot be empty|@"));
                System.out.print("Codelet Name: ");
                codeletName = input.nextLine();
            }

            //Ask codelet group
            Object[] groups = modifiedConfig.getCodelets().stream()
                    .map(CodeletConfig::getGroup).distinct().filter(Objects::nonNull).toArray();
            System.out.println("\nSelect a codelet group to add to:");
            System.out.println("    (0) NONE");
            System.out.println("    (1) Add new Group");
            for (int i = 0; i < groups.length; i++) {
                System.out.println("    (" + (i + 2) + ") " + groups[i]);
            }
            System.out.print(Ansi.AUTO.string("@|bold Select an option (default 0) [0.." + (groups.length + 1) + "]: |@"));
            int groupIdx = Integer.parseInt(input.nextLine());
            String codeletGroup = null;
            if (groupIdx == 1) {
                System.out.print("Enter new Group name: ");
                codeletGroup = input.nextLine();
            }
            if (1 < groupIdx && groupIdx <= groups.length + 1)
                codeletGroup = (String) groups[groupIdx - 2];
            //Ask codelet inputs, outputs and broadcasts
            System.out.print("Enter codelet inputs (comma separated): ");
            String codeletInputs = input.nextLine();
            System.out.print("Enter codelet outputs (comma separated): ");
            String codeletOutputs = input.nextLine();
            System.out.print("Enter codelet broadcast outputs (comma separated): ");
            String codeletBroadcasts = input.nextLine();


            //Create codelet config
            newCodelet = new CodeletConfig(codeletName);
            newCodelet.setGroup(codeletGroup);
            if (!codeletInputs.isBlank())
                for (String inMem : codeletInputs.split(","))
                    newCodelet.addIn(inMem);
            if (!codeletOutputs.isBlank())
                for (String outMem : codeletOutputs.split(","))
                    newCodelet.addOut(outMem);
            if (!codeletBroadcasts.isBlank())
                for (String broadMem : codeletBroadcasts.split(","))
                    newCodelet.addBroadcast(broadMem);
        }

        //Check for non-existing memories in AgentMind
        Set<String> existingMemories = modifiedConfig.getMemories().stream().map(MemoryConfig::getName).collect(Collectors.toSet());
        Set<String> codeletsMemories = new HashSet<>(){{
            addAll(newCodelet.getIn());
            addAll(newCodelet.getOut());
            addAll(newCodelet.getBroadcast());
            removeIf(existingMemories::contains);
        }};
        if (!codeletsMemories.isEmpty()) {
            System.out.println("Some of the memories connected to the codelet are not declared in the current project:");
            System.out.println(codeletsMemories);
            System.out.print(Ansi.AUTO.string("Would you like to add this memories? [@|bold,blue Y|@/n]: "));
            String ans = input.nextLine();
            if (!ans.equalsIgnoreCase("n")) {
                for (String codeletsMemory : codeletsMemories) {
                    createMemoryConfig(openEditor, codeletsMemory);
                }
            }
        }

        modifiedConfig.addCodeletConfig(newCodelet);
        newCodelets.add(newCodelet);
    }

    private String getConfigStringFromEditor(String fileName, String fileTemplate) throws IOException, InterruptedException {
        File edit = new File(rootFolder + "/.cst", fileName);
        edit.getParentFile().mkdirs();
        edit.createNewFile();
        FileWriter writer = new FileWriter(edit);
        writer.write(fileTemplate);
        writer.close();

        ProcessBuilder editor = new ProcessBuilder(
                System.getenv().getOrDefault("EDITOR", "nano"), edit.getAbsolutePath());
        Process editorPrs = null;
        editorPrs = editor.inheritIO().start();
        editorPrs.waitFor();
        String config = Files.lines(edit.toPath()).collect(Collectors.joining("\n"));
        return config;
    }

    private void processCreateMemory(boolean openEditor){
        String memoryName = "";
        if (!openEditor){
            //Ask memory name
            System.out.print("Memory Name: ");
            memoryName = input.nextLine();
            while (memoryName.isBlank()) {
                System.out.println(Ansi.AUTO.string("@|red Memory name cannot be empty|@"));
                System.out.print("Memory Name: ");
                memoryName = input.nextLine();
            }
        }

        createMemoryConfig(openEditor, memoryName);
    }

    private void createMemoryConfig(boolean openEditor, String memoryName){
        MemoryConfig newMemory = null;

        if (openEditor){
            String tab = " ".repeat(memoryName.length());
            String template = """
                    name: %s     # Name for new memory [REQUIRED]
                    type:   %s   # Type of memory (object | container) [REQUIRED]
                    group:  %s   # Group for new memory
                    content:%s   # A single element passed as a map of <type>: <value>
                            %s   # ex:  content:
                            %s   #        int: 42""";
            template = String.format(template, memoryName, tab, tab, tab, tab, tab);
            try {
                String newMemoryConfig = getConfigStringFromEditor("MEMORY", template);
                Yaml parser = new Yaml(new Constructor(MemoryConfig.class, new LoaderOptions()));
                newMemory = parser.load(newMemoryConfig);
            } catch (IOException | InterruptedException e) {
                // TODO
                throw new RuntimeException(e);
            }
        } else {
            //Ask memory type
            System.out.println("\nSelect memory type for " + memoryName + ":");
            System.out.println("    (1) Memory Object");
            System.out.println("    (2) Memory Container");
            System.out.print(Ansi.AUTO.string("@|bold Select an option (default 1): |@"));
            int memTypeIdx = Integer.parseInt(input.nextLine());
            String memoryType = memTypeIdx == 2 ? CONTAINER_TYPE : OBJECT_TYPE;

            //Ask memory group
            Object[] groups = modifiedConfig.getMemories().stream()
                    .map(MemoryConfig::getGroup).distinct().filter(Objects::nonNull).toArray();
            System.out.println("\nSelect a memory group to add to:");
            System.out.println("    (0) NONE");
            System.out.println("    (1) Add new Group");
            for (int i = 0; i < groups.length; i++) {
                System.out.println("    (" + (i + 2) + ") " + groups[i]);
            }
            System.out.print(Ansi.AUTO.string("@|bold Select an option (default 0) [0.." + (groups.length + 1) + "]: |@"));
            int groupIdx = Integer.parseInt(input.nextLine());
            String memoryGroup = null;
            if (groupIdx == 1) {
                System.out.print("Enter new Group name: ");
                memoryGroup = input.nextLine();
            }
            if (1 < groupIdx && groupIdx <= groups.length + 1)
                memoryGroup = (String) groups[groupIdx - 2];
            newMemory = new MemoryConfig(memoryName);
            newMemory.setGroup(memoryGroup);
            newMemory.setType(memoryType);
        }

        if (newMemory != null) modifiedConfig.addMemoryConfig(newMemory);
    }
}
