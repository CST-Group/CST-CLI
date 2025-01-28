package br.unicamp.cst.cli.util;

import java.nio.file.Files;

public class CodeUtils {

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
