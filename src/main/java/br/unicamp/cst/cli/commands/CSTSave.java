package br.unicamp.cst.cli.commands;

import br.unicamp.cst.cli.data.AgentConfig;
import br.unicamp.cst.cli.data.ConfigParser;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;

import java.io.File;
import java.io.FileWriter;
import java.util.concurrent.Callable;

@Command(name = "save", description = "Save current project as YAML template", mixinStandardHelpOptions = true)
public class CSTSave implements Callable<Integer> {

    @Option(names = {"--out", "-o"}, description = "Output file to write YAML template")
    File output;

    @Override
    public Integer call() throws Exception{
        AgentConfig agentConfig = ConfigParser.parseProjectToConfig();

        if (output == null) {
            System.out.println(agentConfig.toYaml());
        } else {
            FileWriter writer = new FileWriter(output);
            writer.write(agentConfig.toYaml());
            writer.close();
        }

        return 0;
    }
}
