package br.unicamp.cst.cli.commands;

import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "run", description = "Executes current project")
public class CSTRun implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        // Get root dir. Traverse up if necessary, until find gradlew
        File currDir = new File(System.getProperty("user.dir"));

        File gradlew = new File(currDir.getAbsolutePath() + "/gradlew");
        while (currDir != null && !gradlew.exists()) {
            gradlew = new File(currDir.getAbsolutePath() + "/gradlew");
            currDir = currDir.getParentFile();
        }

        if (gradlew.exists()){
            Process process = Runtime.getRuntime().exec(currDir.getAbsolutePath() + "/gradlew run -p " + currDir.getAbsolutePath());
            new BufferedReader(new InputStreamReader(process.getInputStream())).lines()
                    .forEach(System.out::println);
        } else {
            System.out.println("No project found! Please execute this command on a folder with a CST project.");
        }
        return 0;
    }
}
