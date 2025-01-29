module br.unicamp.cst.cli {
    requires info.picocli;
    requires org.yaml.snakeyaml;
    requires java.logging;
    requires com.github.javaparser.core;
    requires java.desktop;

    exports br.unicamp.cst.cli;
    opens br.unicamp.cst.cli to info.picocli;
    opens br.unicamp.cst.cli.commands to info.picocli;
    opens br.unicamp.cst.cli.data to org.yaml.snakeyaml, org.assertj.core;
}