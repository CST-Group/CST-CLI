package br.unicamp.cst.cli.data;

import br.unicamp.cst.cli.util.TemplatesBundle;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.observer.PropagatingAstObserver;
import com.github.javaparser.ast.stmt.BlockStmt;

import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;

import static br.unicamp.cst.cli.commands.CSTInit.TAB;

public class CodeletConfig {
    private String name;
    private String group;
    private List<String> in = new ArrayList<>();
    private List<String> out = new ArrayList<>();
    private List<String> broadcast = new ArrayList<>();

    public CodeletConfig() {
    }

    public CodeletConfig(String codeletName) {
        this.setName(codeletName);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public List<String> getIn() {
        return in;
    }

    public void setIn(List<String> in) {
        this.in = in;
    }

    public void addIn(String in) {
        this.in.add(in);
    }

    public List<String> getOut() {
        return out;
    }

    public void setOut(List<String> out) {
        this.out = out;
    }

    public void addOut(String out) {
        this.out.add(out);
    }


    public List<String> getBroadcast() {
        return broadcast;
    }

    public void setBroadcast(List<String> broadcast) {
        this.broadcast = broadcast;
    }

    public void addBroadcast(String mem) {
        this.broadcast.add(mem);
    }

    public String generateCode(String rootPackage) {

        CompilationUnit compilationUnit = new CompilationUnit();

        compilationUnit.setPackageDeclaration(this.getPackage(rootPackage));
        compilationUnit.addImport("br.unicamp.cst.core.entities.Codelet");
        compilationUnit.addImport("br.unicamp.cst.core.entities.Memory");

        ClassOrInterfaceDeclaration codeletClass = compilationUnit.addClass(this.getName()).setPublic(true).addExtendedType("Codelet");

        BlockStmt memoryAccessMethod = new BlockStmt();

        for (String input : this.getIn()){
            codeletClass.addPrivateField("Memory", input);
            MethodCallExpr getInput = new MethodCallExpr("getInput", new StringLiteralExpr(input));
            AssignExpr initMemory = new AssignExpr(new NameExpr(input), getInput, AssignExpr.Operator.ASSIGN);
            memoryAccessMethod.addStatement(initMemory);
        }

        for (String output : this.getOut()){
            codeletClass.addPrivateField("Memory", output);
            MethodCallExpr getOutput = new MethodCallExpr("getOutput", new StringLiteralExpr(output));
            AssignExpr initMemory = new AssignExpr(new NameExpr(output), getOutput, AssignExpr.Operator.ASSIGN);
            memoryAccessMethod.addStatement(initMemory);
        }

        for (String broadcast : this.getBroadcast()){
            codeletClass.addPrivateField("Memory", broadcast);
            MethodCallExpr getBroadcast = new MethodCallExpr("getOutput", new StringLiteralExpr(broadcast));
            AssignExpr initMemory = new AssignExpr(new NameExpr(broadcast), getBroadcast, AssignExpr.Operator.ASSIGN);
            memoryAccessMethod.addStatement(initMemory);
        }

        codeletClass.addMethod("accessMemoryObjects")
                .setPublic(true)
                .addAnnotation("Override")
                .setBody(memoryAccessMethod);
        codeletClass.addMethod("calculateActivation")
                .setPublic(true)
                .addAnnotation("Override")
                .setBody(new BlockStmt());
        codeletClass.addMethod("proc")
                .setPublic(true)
                .addAnnotation("Override")
                .setBody(new BlockStmt());

        return compilationUnit.toString();
    }

    @Override
    public String toString() {
        return "CodeletConfig{" +
                "name='" + name + '\'' +
                ", group='" + group + '\'' +
                ", in=" + in +
                ", out=" + out +
                ", broadcast=" + broadcast +
                '}';
    }

    public String getClassImport(String rootPackage) {
        return this.getPackage(rootPackage) + "." + name;
    }

    public String getPackage(String rootPackage) {
        return rootPackage + ".codelets." + this.group.toLowerCase();
    }
}
