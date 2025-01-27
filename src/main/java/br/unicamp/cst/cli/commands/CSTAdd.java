package br.unicamp.cst.cli.commands;

import br.unicamp.cst.cli.data.AgentConfig;
import br.unicamp.cst.cli.data.CodeletConfig;
import br.unicamp.cst.cli.data.ConfigParser;
import br.unicamp.cst.cli.data.MemoryConfig;
import com.github.javaparser.ParseProblemException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;

import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static br.unicamp.cst.cli.data.MemoryConfig.*;

@Command(name = "add", description = "Adds a new codelet to the project structure")
public class CSTAdd implements Callable<Integer> {

    Scanner input = new Scanner(System.in);
    AgentConfig currAgentConfig = ConfigParser.parseProjectToConfig();
    AgentConfig modifiedConfig = ConfigParser.parseProjectToConfig();
    List<CodeletConfig> newCodelets = new ArrayList<>();

    Path rootFolder;

    @Override
    public Integer call() throws Exception {
        if (findRootFolder()) {
            selectMenu();
            applyChanges();
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

    private void selectMenu() throws IOException {
        //Build string with options and display it
        StringBuilder options = new StringBuilder("Select element to add\n");
        for (VALID_OPTIONS en : VALID_OPTIONS.values()) {
            options.append("    (" + (en.ordinal() + 1) + ") " + en.displayName + "\n");
        }
        options.append(" @|bold Select an option (default 1) [1.."+VALID_OPTIONS.values().length + "]:|@ ");
        System.out.print(Ansi.AUTO.string(options.toString()));

        //Read selected input
        String inputOption = input.nextLine();
        int parsedInput = Integer.parseInt(inputOption);
        VALID_OPTIONS selected = VALID_OPTIONS.values()[0];
        if (!inputOption.isBlank() && parsedInput <= VALID_OPTIONS.values().length)
            selected = VALID_OPTIONS.values()[parsedInput - 1];

        //Process command
        switch (selected) {
            case CODELET:
                processCreateCodelet();
                break;
            case MEMORY_OBJECT:
                processCreateMemory();
                break;
            case MEMORY_CONTAINER:
                break;
        }
        //Re-execute if selected
        System.out.print(Ansi.AUTO.string("Would you like to add another element? [y/@|bold n|@]: "));
        String ans = input.nextLine();
        if (ans.equalsIgnoreCase("y")){
            selectMenu();
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
        System.out.println(modifiedConfig.toString());
        System.out.println(modifiedConfig.toYaml());
        String[] commonBase = currAgentConfig.generateCode().split("\n");
        String[] modifiedCode = modifiedConfig.generateCode().split("\n");
        String[] currAgentCode = {""};
        if (path.exists()){
            currAgentCode = Files.readAllLines(path.toPath()).toArray(currAgentCode);
        }

        StringBuilder mergedCode = new StringBuilder();
        int pB = 0, pC = 0, pM = 0;
        while (pB < commonBase.length){
            String line = commonBase[pB];
            boolean equalCurrent = line.strip().equals(currAgentCode[pC].strip());
            boolean equalModified = line.strip().equals(modifiedCode[pM].strip());
            if (equalCurrent && equalModified){
                mergedCode.append(line).append("\n");
                pC++;
                pM++;
                pB++;
            } else if (!equalCurrent && equalModified){
                mergedCode.append(currAgentCode[pC]).append("\n");
                pC++;
            } else if (equalCurrent && !equalModified){
                 mergedCode.append(modifiedCode[pM]).append("\n");
                 pM++;
            } else if (!equalCurrent && !equalModified){
                mergedCode.append(modifiedCode[pM]).append("\n");
                pM++;
            }
        }
        while (pM < modifiedCode.length){
            mergedCode.append(modifiedCode[pM++]).append("\n");
        }
        while (pC < currAgentCode.length){
            mergedCode.append(currAgentCode[pC++]).append("\n");
        }

        FileWriter writer = new FileWriter(path);
        writer.write(mergedCode.toString());
        writer.close();
    }

    private void processCreateCodelet(){
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
            System.out.println("    (" + (i+2) + ") " + groups[i]);
        }
        System.out.print(Ansi.AUTO.string("@|bold Select an option (default 0) [0.." + (groups.length + 1) + "]: |@"));
        int groupIdx = Integer.parseInt(input.nextLine());
        String codeletGroup = null;
        if (groupIdx == 1){
            System.out.print("Enter new Group name: ");
            codeletGroup = input.nextLine();
        }
        if (1 < groupIdx && groupIdx <= groups.length + 1)
            codeletGroup = (String) groups[groupIdx-2];
        //Ask codelet inputs, outputs and broadcasts
        System.out.print("Enter codelet inputs (comma separated): ");
        String codeletInputs = input.nextLine();
        System.out.print("Enter codelet outputs (comma separated): ");
        String codeletOutputs = input.nextLine();
        System.out.print("Enter codelet broadcast outputs (comma separated): ");
        String codeletBroadcasts = input.nextLine();

        Set<String> codeletsMemories = new HashSet<>();
        if (!codeletInputs.isBlank())
            codeletsMemories.addAll(List.of(codeletInputs.split(",")));
        if (!codeletOutputs.isBlank())
            codeletsMemories.addAll(List.of(codeletOutputs.split(",")));
        if (!codeletBroadcasts.isBlank())
            codeletsMemories.addAll(List.of(codeletBroadcasts.split(",")));

        Set<String> existingMemories = modifiedConfig.getMemories().stream().map(MemoryConfig::getName).collect(Collectors.toSet());
        codeletsMemories.removeIf(existingMemories::contains);

        if (!codeletsMemories.isEmpty()){
            System.out.println("Some of the memories connected to the codelet are not declared in the current project:");
            System.out.println(codeletsMemories);
            System.out.print(Ansi.AUTO.string("Would you like to add this memories? [@|bold Y|@/n]: "));
            String ans = input.nextLine();
            if (!ans.equalsIgnoreCase("n")){
                codeletsMemories.forEach(this::createMemoryConfig);
            }
        }

        //Create codelet config
        CodeletConfig newCodelet = new CodeletConfig(codeletName);
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

        modifiedConfig.addCodeletConfig(newCodelet);
        newCodelets.add(newCodelet);
    }

    private void processCreateMemory(){
        //Ask memory name
        System.out.print("Memory Name: ");
        String memoryName = input.nextLine();
        while (memoryName.isBlank()) {
            System.out.println(Ansi.AUTO.string("@|red Memory name cannot be empty|@"));
            System.out.print("Memory Name: ");
            memoryName = input.nextLine();
        }

        createMemoryConfig(memoryName);
    }

    private void createMemoryConfig(String memoryName){
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
            System.out.println("    (" + (i+2) + ") " + groups[i]);
        }
        System.out.print(Ansi.AUTO.string("@|bold Select an option (default 0) [0.." + (groups.length + 1) + "]: |@"));
        int groupIdx = Integer.parseInt(input.nextLine());
        String memoryGroup = null;
        if (groupIdx == 1){
            System.out.print("Enter new Group name: ");
            memoryGroup = input.nextLine();
        }
        if (1 < groupIdx && groupIdx <= groups.length + 1)
            memoryGroup = (String) groups[groupIdx-2];

        MemoryConfig newMemoryConfig = new MemoryConfig(memoryName);
        newMemoryConfig.setGroup(memoryGroup);
        newMemoryConfig.setType(memoryType);

        modifiedConfig.addMemoryConfig(newMemoryConfig);
    }

    enum VALID_OPTIONS {
        CODELET("Codelet"),
        MEMORY_OBJECT("Memory Object"),
        MEMORY_CONTAINER("Memory Container");

        public String displayName;

        VALID_OPTIONS(String displayName) {
            this.displayName = displayName;
        }
    }
}
