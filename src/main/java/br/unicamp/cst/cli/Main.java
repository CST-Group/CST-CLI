package br.unicamp.cst.cli;

import br.unicamp.cst.cli.commands.CSTInit;
import br.unicamp.cst.cli.commands.CSTRun;
import br.unicamp.cst.cli.commands.CSTSave;
import br.unicamp.cst.cli.commands.CSTAdd;
import picocli.CommandLine.Command;
import picocli.CommandLine;

@Command(name = "cst",
        synopsisSubcommandLabel = "COMMAND",
        mixinStandardHelpOptions = true,
        version = "CST CLI - Beta version 0.2",
        subcommands = {
            CSTInit.class,
            CSTSave.class,
            CSTRun.class,
        })
public class Main implements Runnable {
    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        throw new CommandLine.ParameterException(spec.commandLine(), "Missing required subcommand");
    }
}