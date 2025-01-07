package br.unicamp.cst.cli.data;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import static br.unicamp.cst.cli.data.AgentConfig.getVarName;
import static br.unicamp.cst.cli.util.Constants.*;

public class ConfigParser {

    public static AgentConfig parseProjectToConfig() {
        // Get root dir. Traverse up if necessary, until find folder src/
        File currDir = new File(System.getProperty("user.dir"));

        File srcFolder = new File(currDir.getAbsolutePath() + "/src");
        while (currDir != null && !srcFolder.exists()) {
            srcFolder = new File(currDir.getAbsolutePath() + "/src");
            currDir = currDir.getParentFile();
        }

        if (!srcFolder.exists()) return new AgentConfig();

        //Read settings.gradle file to collect project name for agent config
        File gradleSettings = new File(currDir.getAbsolutePath() + "/settings.gradle");
        String projectName = null;
        if (gradleSettings.exists()) {
            try {
                for (String line : Files.lines(gradleSettings.toPath()).toList()) {
                    if (line.startsWith("rootProject.name"))
                        projectName = line.substring(line.indexOf("'") + 1, line.lastIndexOf("'")).strip();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


        //Traverse folders and get packageName
        //Find AgentMind file
        srcFolder = new File(srcFolder, "main");
        if (!srcFolder.exists())
            return new AgentConfig();

        File agentMindFile = new File(srcFolder, "AgentMind.java");
        while (!agentMindFile.exists() && srcFolder.listFiles() != null) {
            srcFolder = srcFolder.listFiles()[0];
            agentMindFile = new File(srcFolder, "AgentMind.java");
        }

        if (!agentMindFile.exists())
            return new AgentConfig();

        ConstructorDeclaration agentConstructor = null;
        String packageName = null;
        try {
            CompilationUnit cu = StaticJavaParser.parse(agentMindFile);
            Optional<PackageDeclaration> packageNameOpt = cu.getPackageDeclaration();
            packageName = packageNameOpt.isPresent() ? packageNameOpt.get().getNameAsString() : null;
            VoidVisitor<List<ConstructorDeclaration>> findConstructorVisitor = new ConstructorCollector();
            List<ConstructorDeclaration> agentConstructors = new ArrayList<>();
            findConstructorVisitor.visit(cu, agentConstructors);
            if (agentConstructors.size() == 1){
                agentConstructor = agentConstructors.get(0);
            } else {
                System.out.println("Multiple Constructors detected for Agent Mind");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        AgentConfig testConfig = new AgentConfig();
        testConfig.setProjectName(projectName);
        testConfig.setPackageName(packageName);
        agentConstructor.accept(new AgentConfigCollector(), testConfig);
        return testConfig;

    }

    static class ConstructorCollector extends VoidVisitorAdapter<List<ConstructorDeclaration>>{

        @Override
        public void visit(ConstructorDeclaration cd, List<ConstructorDeclaration> constructor){
            super.visit(cd, constructor);
            constructor.add(cd);
        }
    }

    static class AgentConfigCollector extends VoidVisitorAdapter<AgentConfig>{
        Map<String, String> codeletVariables = new HashMap<>();

        @Override
        public void visit(VariableDeclarator vd, AgentConfig agentConfig){
            if (vd.getTypeAsString().equals(MEMORY_OBJECT_TYPE)) {
                MemoryConfig memoryConfig = agentConfig.findMemoryOrCreate(vd.getNameAsString());
                memoryConfig.setType(MemoryConfig.OBJECT_TYPE);
            } else if (vd.getTypeAsString().equals(MEMORY_CONTAINER_TYPE)) {
                System.out.println(vd.toString());
                MemoryConfig memoryConfig = agentConfig.findMemoryOrCreate(vd.getNameAsString());
                memoryConfig.setType(MemoryConfig.CONTAINER_TYPE);
            } else if (vd.getTypeAsString().equals(MEMORY_BASE_TYPE)) {
                agentConfig.findMemoryOrCreate(vd.getNameAsString());
            } else if (vd.getTypeAsString().equals("Codelet")) {
                if (vd.getInitializer().isPresent()) {
                    String codeletName = vd.getInitializer().get().asObjectCreationExpr().getType().asString();
                    CodeletConfig codeletConfig = agentConfig.findCodeletOrCreate(codeletName);
                    String varName = vd.getNameAsString();
                    codeletVariables.put(varName, codeletName);
                }
            }
            super.visit(vd, agentConfig);
        }

        @Override
        public void visit(MethodCallExpr mc, AgentConfig agentConfig){
            if (mc.getNameAsString().equals(CREATE_MEMORY_OBJECT_FUNCTION)){
                NodeList<Expression> args = mc.getArguments();
                if (!args.isEmpty()){
                    String memoryName = args.get(0).asStringLiteralExpr().asString();
                    Optional<MemoryConfig> memoryConfig = agentConfig.findMemory(memoryName);
                    memoryConfig.ifPresent(config -> config.setType(MemoryConfig.OBJECT_TYPE));
                    memoryConfig.ifPresent(config -> config.setName(memoryName));
                }
            } else if (mc.getNameAsString().equals(CREATE_MEMORY_CONTAINER_FUNCTION)){
                NodeList<Expression> args = mc.getArguments();
                if (!args.isEmpty()){
                    String memoryName = args.get(0).asStringLiteralExpr().asString();
                    Optional<MemoryConfig> memoryConfig = agentConfig.findMemory(memoryName);
                    memoryConfig.ifPresent(config -> config.setType(MemoryConfig.CONTAINER_TYPE));
                    memoryConfig.ifPresent(config -> config.setName(memoryName));
                }
            } else if (mc.getNameAsString().equals(REGISTER_MEMORY_FUNCTION)){
                NodeList<Expression> args = mc.getArguments();
                if (!args.isEmpty()) {
                    String memoryName = args.get(0).toString();
                    String memoryGroup = args.get(1).toString().replaceAll("\"","");
                    Optional<MemoryConfig> memoryConfig = agentConfig.findMemory(memoryName);
                    memoryConfig.ifPresent(config -> config.setGroup(memoryGroup));
                }
            } else if (mc.getNameAsString().equals("registerCodelet")) {
                NodeList<Expression> args = mc.getArguments();
                if (!args.isEmpty()){
                    String codeletVarName = args.get(0).toString();
                    String codeletName = codeletVariables.get(codeletVarName);
                    String codeletGroup = args.get(1).toString().replaceAll("\"","");
                    Optional<CodeletConfig> codeletConfig = agentConfig.findCodelet(codeletName);
                    codeletConfig.ifPresent(config -> config.setGroup(codeletGroup));
                }
            } else if (mc.getNameAsString().equals("addInput")) {
                addMemoryToCodelet(mc, agentConfig, 1);
            } else if (mc.getNameAsString().equals("addOutput")) {
                addMemoryToCodelet(mc, agentConfig, 2);
            } else if (mc.getNameAsString().equals("addBroadcast")) {
                addMemoryToCodelet(mc, agentConfig, 3);
            }
            super.visit(mc, agentConfig);
        }

        private void addMemoryToCodelet(MethodCallExpr mc, AgentConfig agentConfig, int type) {
            if (mc.getScope().isPresent()){
                String codeletVarName = mc.getScope().get().toString();
                String codeletName = codeletVariables.get(codeletVarName);
                String memoryName = mc.getArguments().get(0).toString();
                Optional<CodeletConfig> codeletConfig = agentConfig.findCodelet(codeletName);
                Optional<MemoryConfig> memoryConfig = agentConfig.findMemory(memoryName);
                if (codeletConfig.isPresent() && memoryConfig.isPresent()){
                    if (type == 1)
                        codeletConfig.get().addIn(memoryConfig.get().getName());
                    if (type == 2)
                        codeletConfig.get().addOut(memoryConfig.get().getName());
                    if (type == 3)
                        codeletConfig.get().addBroadcast(memoryConfig.get().getName());
                }
            }
        }
    }
}