package commands;

import br.unicamp.cst.cli.Main;
import br.unicamp.cst.cli.data.AgentConfig;
import br.unicamp.cst.cli.data.CodeletConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class CSTSaveTest {

    @TempDir
    Path tempDir;

    String dirName;
    int exitCode;
    final PrintStream originalOut = System.out;
    final PrintStream originalErr = System.err;
    final InputStream originalIn = System.in;
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final ByteArrayOutputStream err = new ByteArrayOutputStream();
    final InputStream in = new ByteArrayInputStream(new byte[0]);

    String yamlConfig = """
                projectName: MyProject
                packageName: my.project
                codelets:
                  - name: TestCodelet
                    group: test
                    in: [MemOne]
                    out: [MemTwo]
                    broadcast: [MemThree]
                memories:
                  - content: null
                    group: test
                    name: MemOne
                    type: object
                  - content: null
                    group: test
                    name: MemTwo
                    type: container
                  - content: null
                    group: test
                    name: MemThree
                    type: object""";

    @BeforeEach
    public void setDir() {
        System.setProperty("user.dir", tempDir.toString());
        String[] dirs = tempDir.toString().split("/");
        dirName = dirs[dirs.length - 1];
    }

    @BeforeEach
    public void setUpStreams(){
        out.reset();
        err.reset();
        System.setOut(new PrintStream(out));
        System.setErr(new PrintStream(err));
    }

    @AfterEach
    public void restoreStreams(){
        System.setOut(originalOut);
        System.setErr(originalErr);
        System.setIn(originalIn);
    }

    private void setInput(String data){
        System.setIn(new ByteArrayInputStream(data.getBytes())); // Inject mock Scanner
    }

    private String readFileFromTmpDir(String file) throws IOException {
        return Files.lines(new File(tempDir.toString(), file).toPath()).collect(Collectors.joining("\n"));
    }

    private File createMockYAMLFile() {
        // Create a mock YAML config file
        File configFile = new File(tempDir.toString(), "test_config.yaml");
        try {
            FileWriter writer = new FileWriter(configFile);
            writer.write(yamlConfig);
            writer.close();
        } catch (IOException e) {
            fail("Failed to create mock config file");
        }
        return configFile;
    }

    @Test
    public void testSavePrintOuput(){
        File configFile = createMockYAMLFile();
        exitCode = new CommandLine(new Main()).execute("init", "--file", configFile.toString());
        assertThat(exitCode).isEqualTo(0);

        exitCode = new CommandLine(new Main()).execute("save");
        assertThat(exitCode).isEqualTo(0);

        Yaml yamlParser = new Yaml(new Constructor(AgentConfig.class, new LoaderOptions()));
        AgentConfig newAgentConfig = yamlParser.load(out.toString());
        AgentConfig originalAgentConfig = yamlParser.load(yamlConfig);

        assertThat(newAgentConfig).usingRecursiveComparison().isEqualTo(originalAgentConfig);
    }

    @Test
    public void testSaveToFile() throws IOException {
        File configFile = createMockYAMLFile();
        exitCode = new CommandLine(new Main()).execute("init", "--file", configFile.toString());
        assertThat(exitCode).isEqualTo(0);

        String savedConfigFile = "newConfig.yaml";
        exitCode = new CommandLine(new Main()).execute("save", "--out", tempDir + "/" + savedConfigFile);
        assertThat(exitCode).isEqualTo(0);

        Yaml yamlParser = new Yaml(new Constructor(AgentConfig.class, new LoaderOptions()));
        AgentConfig newAgentConfig = yamlParser.load(readFileFromTmpDir(savedConfigFile));
        AgentConfig originalAgentConfig = yamlParser.load(yamlConfig);

        assertThat(newAgentConfig).usingRecursiveComparison().isEqualTo(originalAgentConfig);
    }
}
