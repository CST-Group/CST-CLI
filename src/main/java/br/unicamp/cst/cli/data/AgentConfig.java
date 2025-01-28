package br.unicamp.cst.cli.data;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

import java.util.*;
import java.util.function.Predicate;

import static br.unicamp.cst.cli.data.MemoryConfig.CONTAINER_TYPE;
import static br.unicamp.cst.cli.data.MemoryConfig.OBJECT_TYPE;

public class AgentConfig {

    private String projectName;
    private String packageName;

    private List<CodeletConfig> codelets = new ArrayList<>();
    private List<MemoryConfig> memories = new ArrayList<>();

    private static Type memoryType = new ClassOrInterfaceType(null, "Memory");
    private static Type codeletType = new ClassOrInterfaceType(null, "Codelet");

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public List<CodeletConfig> getCodelets() {
        return codelets;
    }

    public void setCodelets(List<CodeletConfig> codelets) {
        this.codelets = codelets;
    }

    public void addCodeletConfig(CodeletConfig newCodelet) {
        this.codelets.add(newCodelet);
    }

    public List<MemoryConfig> getMemories() {
        return memories;
    }

    public void setMemories(List<MemoryConfig> memories) {
        this.memories = memories;
    }

    public void addMemoryConfig(MemoryConfig newMemory) {
        this.memories.add(newMemory);
    }

    public String generateCode() {
        CompilationUnit compilationUnit = new CompilationUnit();

        //Imports
        compilationUnit.setPackageDeclaration(this.packageName);
        for (CodeletConfig codelet : this.getCodelets()) {
            compilationUnit.addImport(codelet.getClassImport(this.packageName));
        }
        compilationUnit.addImport("br.unicamp.cst.core.entities.Codelet");
        compilationUnit.addImport("br.unicamp.cst.core.entities.Memory");
        compilationUnit.addImport("br.unicamp.cst.core.entities.Mind");

        ClassOrInterfaceDeclaration agentClass = compilationUnit.addClass("AgentMind").setPublic(true).addExtendedType("Mind");

        //Constructor
        ConstructorDeclaration constructor = agentClass.addConstructor();
        BlockStmt constructorBody = new BlockStmt();
        MethodCallExpr superCall = new MethodCallExpr();
        superCall.setName("super");
        constructorBody.addStatement(superCall);
        //Jump line
        constructorBody.addStatement(new EmptyStmt());

        //Codelet Groups
        Iterator<String> groups = getCodelets().stream()
                .map(CodeletConfig::getGroup).distinct().filter(Objects::nonNull).iterator();
        boolean first = true;
        while (groups.hasNext()) {
            MethodCallExpr codeletGroupCall = new MethodCallExpr();
            if (first) {
                codeletGroupCall.addOrphanComment(new LineComment(" Codelets Groups Declaration"));
                first = false;
            }
            codeletGroupCall.setName("createCodeletGroup");
            codeletGroupCall.addArgument(new StringLiteralExpr(groups.next()));
            constructorBody.addStatement(codeletGroupCall);
        }

        // Jump Line
        constructorBody.addStatement(new EmptyStmt());

        //Memory Groups
        Iterator<String> uniqueMemoryGroups = getMemories().stream()
                .map(MemoryConfig::getGroup).distinct().filter(Objects::nonNull).iterator();
        first = true;
        while (uniqueMemoryGroups.hasNext()) {
            MethodCallExpr memoryGroupCall = new MethodCallExpr();
            if (first) {
                memoryGroupCall.addOrphanComment(new LineComment(" Memory Groups Declaration"));
                first = false;
            }
            memoryGroupCall.setName("createMemoryGroup");
            memoryGroupCall.addArgument(new StringLiteralExpr(uniqueMemoryGroups.next()));
            constructorBody.addStatement(memoryGroupCall);
        }
        //Jump line
        constructorBody.addStatement(new EmptyStmt());

        //Memory Declarations
        first = true;
        for (MemoryConfig memory : this.getMemories()) {
            String memoryVar = getVarName(memory.getName());
            // Declare memory variable
            VariableDeclarationExpr memoryDeclaration = new VariableDeclarationExpr(memoryType, memoryVar);
            if (first) {
                memoryDeclaration.addOrphanComment(new LineComment(" Memories Initialization"));
                first = false;
            }
            constructorBody.addStatement(memoryDeclaration);
        }
        //Jump line
        constructorBody.addStatement(new EmptyStmt());

        first = true;
        for (MemoryConfig memory : this.getMemories()) {
            String memoryVar = getVarName(memory.getName());
            // Initialize memory content
            AssignExpr initializeContent = null;
            if (memory.getContent() != null){
                String contentType = memory.getContent().keySet().iterator().next();
                String contentInitialValue = memory.getContent().get(contentType).toString();
                LiteralExpr assignValue;
                if (contentInitialValue == null) {
                    assignValue = new NullLiteralExpr();
                } else {
                    try {
                        double d = Double.parseDouble(contentInitialValue);
                        assignValue = new DoubleLiteralExpr(contentInitialValue);
                    } catch (NumberFormatException nfe) {
                        if (contentType.equalsIgnoreCase("boolean"))
                            assignValue = new BooleanLiteralExpr(contentInitialValue.equals("true"));
                       else
                            assignValue = new StringLiteralExpr(contentInitialValue);
                    }
                }
                Type memoryContentType = new ClassOrInterfaceType(null, contentType);
                VariableDeclarationExpr memoryContentDeclaration = new VariableDeclarationExpr(memoryContentType, memoryVar + "Content");
                initializeContent = new AssignExpr(memoryContentDeclaration, assignValue, AssignExpr.Operator.ASSIGN);
                constructorBody.addStatement(initializeContent);
            }
            // Initialize memory object
            MethodCallExpr createMemoryCall = new MethodCallExpr();
            if (memory.getType().equals(OBJECT_TYPE))
                createMemoryCall.setName("createMemoryObject");
            else if (memory.getType().equals(CONTAINER_TYPE)) {
                createMemoryCall.setName("createMemoryContainer");
            }
            createMemoryCall.addArgument(new StringLiteralExpr(memory.getName()));
            //Add content initialization if defined
            if (initializeContent != null)
                createMemoryCall.addArgument(new NameExpr(memoryVar + "Content"));
            AssignExpr initializeMemory = new AssignExpr(new NameExpr(memoryVar), createMemoryCall, AssignExpr.Operator.ASSIGN);
            if (first) {
                initializeMemory.addOrphanComment(new LineComment(" Memories Initialization"));
                first = false;
            }
            constructorBody.addStatement(initializeMemory);
            //Register memory to group
            if (memory.getGroup() != null) {
                MethodCallExpr registerMemoryCall = new MethodCallExpr("registerMemory",
                        new NameExpr(memoryVar),
                        new StringLiteralExpr(memory.getGroup()));
                constructorBody.addStatement(registerMemoryCall);
            }
        }
        //Jump line
        constructorBody.addStatement(new EmptyStmt());

        first = true;
        for (CodeletConfig codelet : this.getCodelets()) {
            String codeletVarName = getVarName(codelet.getName());
            ClassOrInterfaceType codeletAsType = new ClassOrInterfaceType(null, codelet.getName());
            VariableDeclarator codeletInit = new VariableDeclarator(codeletType, codeletVarName);
            codeletInit.setInitializer(new ObjectCreationExpr(null, codeletAsType, new NodeList<>()));
            VariableDeclarationExpr expr = new VariableDeclarationExpr(codeletInit);
            if (first) {
                expr.addOrphanComment(new LineComment(" Codelets Initialization\n"));
                first = false;
            }
            constructorBody.addStatement(expr);
            for (String inMemory : codelet.getIn()) {
                MethodCallExpr inExpr = new MethodCallExpr(new NameExpr(codeletVarName), "addInput");
                inExpr.addArgument(new NameExpr(getVarName(inMemory)));
                constructorBody.addStatement(inExpr);
            }
            for (String outMemory : codelet.getOut()) {
                MethodCallExpr outExpr = new MethodCallExpr(new NameExpr(codeletVarName), "addOutput");
                outExpr.addArgument(new NameExpr(getVarName(outMemory)));
                constructorBody.addStatement(outExpr);
            }
            for (String broadcastMemory : codelet.getBroadcast()) {
                MethodCallExpr broadcastExpr = new MethodCallExpr(new NameExpr(codeletVarName), "addBroadcast");
                broadcastExpr.addArgument(new NameExpr(getVarName(broadcastMemory)));
                constructorBody.addStatement(broadcastExpr);
            }

            constructorBody.addStatement(new MethodCallExpr("insertCodelet", new NameExpr(codeletVarName)));
            if (codelet.getGroup() != null)
                constructorBody.addStatement(new MethodCallExpr("registerCodelet",
                        new NameExpr(codeletVarName), new StringLiteralExpr(codelet.getGroup())));

            //Jump line
            constructorBody.addStatement(new EmptyStmt());
        }

        constructorBody.addStatement(getForEachStmt());
        constructorBody.addStatement(new ExpressionStmt(new MethodCallExpr("start")));

        constructor.setBody(constructorBody);
        String unitCode = compilationUnit.toString();
        unitCode = unitCode.replaceAll(" ;", "");

        return unitCode;
    }

    private static ForEachStmt getForEachStmt() {
        ForEachStmt setCodeletsTime = new ForEachStmt();
        setCodeletsTime.setVariable(new VariableDeclarationExpr(codeletType, "c"));
        MethodCallExpr getCodeletRack = new MethodCallExpr(new ThisExpr(), "getCodeRack");
        setCodeletsTime.setIterable(new MethodCallExpr(getCodeletRack, "getAllCodelets"));
        NameExpr c = new NameExpr("c");
        NodeList<Expression> arguments = new NodeList<>(new IntegerLiteralExpr(200));
        MethodCallExpr setTimeStep = new MethodCallExpr(c, "setTimeStep", arguments);
        setCodeletsTime.setBody(new BlockStmt().addStatement(setTimeStep));
        return setCodeletsTime;
    }

    public static String getVarName(String name) {
        char[] split = name.toCharArray();
        split[0] = Character.toLowerCase(split[0]);
        return new String(split);
    }

    @Override
    public String toString() {
        return "AgentConfig{" +
                "projectName='" + projectName + "'" +
                "packageName='" + packageName + "'" +
                ", codelets=" + codelets +
                ", memories=" + memories +
                '}';
    }

    public String toYaml() {
        StringBuilder sb = new StringBuilder();
        if (projectName != null)
            sb.append("projectName: ").append(projectName).append("\n");
        if (packageName != null)
            sb.append("packageName: ").append(packageName).append("\n");

        sb.append("\ncodelets:\n");
        for (CodeletConfig codelet : codelets) {
            sb.append("  ")
                    .append("- name: ")
                    .append(codelet.getName())
                    .append("\n");
            sb.append("    ")
                    .append("group: ")
                    .append(codelet.getGroup())
                    .append("\n");
            sb.append("    ")
                    .append("in: ")
                    .append(Arrays.toString(codelet.getIn().toArray()))
                    .append("\n");
            sb.append("    ")
                    .append("out: ")
                    .append(Arrays.toString(codelet.getOut().toArray()))
                    .append("\n");
            sb.append("    ")
                    .append("broadcast: ")
                    .append(Arrays.toString(codelet.getBroadcast().toArray()))
                    .append("\n");
        }

        sb.append("\nmemories:\n");
        for (MemoryConfig memory : memories) {
            sb.append("  ")
                    .append("- name: ")
                    .append(memory.getName())
                    .append("\n");
            sb.append("    ")
                    .append("type: ")
                    .append(memory.getType())
                    .append("\n");
            sb.append("    ")
                    .append("content: ")
                    .append(memory.getContent())
                    .append("\n");
            sb.append("    ")
                    .append("group: ")
                    .append(memory.getGroup())
                    .append("\n");
        }

        return sb.toString();
    }

    public MemoryConfig findMemoryOrCreate(String nameAsString) {
        Optional<MemoryConfig> memoryConfig = this.memories.stream().filter(m -> m.getName().equalsIgnoreCase(nameAsString)).findFirst();
        if (memoryConfig.isPresent()) {
            return memoryConfig.get();
        } else {
            MemoryConfig newMemory = new MemoryConfig(nameAsString);
            this.memories.add(newMemory);
            return newMemory;
        }
    }

    public Optional<MemoryConfig> findMemory(String memoryName) {
        return this.memories.stream().filter(m -> m.getName().equalsIgnoreCase(memoryName)).findFirst();
    }

    public CodeletConfig findCodeletOrCreate(String codeletName) {
        Optional<CodeletConfig> codeletConfig = this.codelets.stream().filter(m -> m.getName().equalsIgnoreCase(codeletName)).findFirst();
        if (codeletConfig.isPresent()) {
            return codeletConfig.get();
        } else {
            CodeletConfig newCodelet = new CodeletConfig(codeletName);
            this.codelets.add(newCodelet);
            return newCodelet;
        }
    }

    public Optional<CodeletConfig> findCodelet(String codeletName) {
        return this.codelets.stream().filter(c -> c.getName().equalsIgnoreCase(codeletName)).findFirst();
    }

    public AgentConfig mergeWith(AgentConfig otherAgentConfig){
        for (CodeletConfig codeletConfig : otherAgentConfig.codelets){
            Optional<CodeletConfig> optCodelet = findCodelet(codeletConfig.getName());
            if (!optCodelet.isPresent()){
                this.codelets.add(codeletConfig);
            }
        }

        for (MemoryConfig memoryConfig : otherAgentConfig.memories){
            Optional<MemoryConfig> optMemory = findMemory(memoryConfig.getName());
            if (!optMemory.isPresent()){
                this.memories.add(memoryConfig);
            }
        }
        return this;
    }

}
