package br.unicamp.cst.cli.util;

import br.unicamp.cst.cli.data.AgentConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CodeUtils {

    public static String getCurrentAgentMindCode(Path rootFolder, AgentConfig currAgentConfig) throws IOException {
        File path = new File(rootFolder + "/src/main/java/" + currAgentConfig.getPackageName().replace(".", "/") + "/AgentMind.java");
        //Base version of code to compare with original (may contain comments and auxiliary functions)
        //and with the modified version of project.
        String currAgentCode = "";
        if (path.exists()){
            currAgentCode = String.join("\n", Files.readAllLines(path.toPath()));
        }
        return currAgentCode;
    }

    public static String generateMergedCode(AgentConfig base, AgentConfig modified, String current){
        return mergeCodes(base.generateCode(), modified.generateCode(), current);
    }

    public static String mergeCodes(String base, String modified, String current){
        String[] commonBase = base.split("\n");
        String[] modifiedCode = modified.split("\n");
        String[] currAgentCode = current.split("\n");

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

        return mergedCode.toString();
    }
}
