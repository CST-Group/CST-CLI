package br.unicamp.cst.cli.commands;

import br.unicamp.cst.cli.data.AgentConfig;
import br.unicamp.cst.cli.data.CodeletConfig;
import br.unicamp.cst.cli.data.ConfigParser;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Command(name = "add", description = "Adds a new codelet to the project structure")
public class CSTAdd implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        //Build string with options and display it
        StringBuilder options = new StringBuilder("Select element to add\n");
        for (VALID_OPTIONS en : VALID_OPTIONS.values()) {
            options.append("    (" + (en.ordinal() + 1) + ") " + en.displayName + "\n");
        }
        options.append(" @|bold Select an option (default 1) [1..3]:|@ ");
        System.out.print(Ansi.AUTO.string(options.toString()));

        //Read selected input
        Scanner input = new Scanner(System.in);
        String inputOption = input.nextLine();
        int parsedInput = Integer.parseInt(inputOption);
        VALID_OPTIONS selected = VALID_OPTIONS.values()[0];
        if (!inputOption.isBlank() && parsedInput <= VALID_OPTIONS.values().length)
            selected = VALID_OPTIONS.values()[parsedInput - 1];

        //Process command
        AgentConfig currAgentConfig = ConfigParser.parseProjectToConfig();
        Object[] groups = currAgentConfig.getCodelets().stream()
                .map(CodeletConfig::getGroup).distinct().filter(Objects::nonNull).toArray();
        switch (selected) {
            case CODELET:
                //Ask codelet name
                System.out.print("Codelet Name: ");
                String codeletName = input.nextLine();
                while (codeletName.isBlank()) {
                    System.out.println(Ansi.AUTO.string("@|red Codelet name cannot be empty|@"));
                    System.out.print("Codelet Name: ");
                    codeletName = input.nextLine();
                }
                System.out.println(codeletName);
                //Ask codelet group
                System.out.println("Select a codelet group to add to:\n");
                System.out.println("    (0) NONE");
                for (int i = 0; i < groups.length; i++) {
                    System.out.println("    (" + (i+1) + ") " + groups[i]);
                }
                System.out.print(Ansi.AUTO.string("@|bold Select an option (default 0) [0.." + groups.length + "]:|@"));
                int groupIdx = Integer.parseInt(input.nextLine());
                String codeletGroup = null;
                if (0 < groupIdx && groupIdx <= groups.length)
                    codeletGroup = (String) groups[groupIdx-1];
                System.out.println(codeletGroup);
                //Ask codelet inputs, outputs and broadcasts
                System.out.print("Codelet inputs (comma separated): ");
                String codeletInputs = input.nextLine();
                System.out.print("Codelet outputs (comma separated): ");
                String codeletOutputs = input.nextLine();
                System.out.print("Codelet broadcast outputs (comma separated): ");
                String codeletBroadcasts = input.nextLine();

                //Create codelet config
                CodeletConfig newCodelet = new CodeletConfig(codeletName);
                break;
            case MEMORY_OBJECT:
                break;
            case MEMORY_CONTAINER:
                break;

        }
        return 0;
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
