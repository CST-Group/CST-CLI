package commands;

import br.unicamp.cst.cli.Main;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class CSTInitTest {

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

    private void initBasicTestProject(){
        setInput("TestProject\ntestProject\n");
        exitCode = new CommandLine(new Main()).execute("init");
    }

    private void initBasicTestProject(String... args){
        setInput("TestProject\ntestProject\n");
        String[] initArgs = {"init"};
        initArgs = Stream.of(initArgs, args).flatMap(Stream::of).toArray(String[]::new);
        exitCode = new CommandLine(new Main()).execute(initArgs);
    }

    private void assertPathsExists(List<String> expectedPaths) {
        for (String expectedPath : expectedPaths) {
            File file = new File(tempDir + expectedPath);
            assertTrue(file.exists(), "File or directory does not exist: " + expectedPath);
        }
    }

    private void assertPathsNotExists(List<String> expectedPaths) {
        for (String expectedPath : expectedPaths) {
            File file = new File(tempDir + expectedPath);
            assertFalse(file.exists(), "File or directory does not exist: " + expectedPath);
        }
    }

    private String readFileFromTmpDir(String file) throws IOException {
        return Files.lines(new File(tempDir.toString(), file).toPath()).collect(Collectors.joining("\n"));
    }

    @Test
    public void testInitAsksForRequiredParams(){
        initBasicTestProject();
        assertEquals(0, exitCode);
        assertEquals("Enter project name (default: " + dirName + ") : Enter package name (default: testproject): ", out.toString());
        File[] files = tempDir.toFile().listFiles();
        assertNotNull(files);
        assertTrue(files.length > 0);
    }

    @Test
    public void testDirectoryStructure(){
        initBasicTestProject();
        assertEquals(0, exitCode);
        List<String> expectedPaths = Arrays.asList(
                "/src/main/java/testProject/Main.java",
                "/src/main/java/testProject/AgentMind.java",
                "/src/main/java/testProject/codelets",
                "/src/main/resources",
                "/src/test/java",
                "/build.gradle",
                "/gradle/wrapper",
                "/gradlew",
                "/settings.gradle"
        );
        assertPathsExists(expectedPaths);
    }


    @Test
    public void testOverwriteMessage(){
        // Set up a non-empty directory by creating a dummy file
        new File(tempDir.toString(), "dummyFile.txt").mkdirs();

        // Set input to simulate choosing to overwrite and run command
        setInput("1\nTestProject\ntestProject\n");
        exitCode = new CommandLine(new Main()).execute("init");
        assertEquals(0, exitCode);

        assertTrue(out.toString().contains("WARNING: This directory is not empty"));
        File[] files = tempDir.toFile().listFiles();
        assertNotNull(files);
        assertTrue(files.length > 0);
    }

    @Test
    public void testCorrectYAMLConfigParsing() throws IOException {
        File configFile = createMockYAMLFile();
        exitCode = new CommandLine(new Main()).execute("init", "--file", configFile.toString());
        assertEquals(0, exitCode);

        List<String> expectedPaths = Arrays.asList(
                "/src/main/java/my/project/Main.java",
                "/src/main/java/my/project/AgentMind.java",
                "/src/main/java/my/project/codelets",
                "/src/main/java/my/project/codelets/test/TestCodelet.java"
        );
        assertPathsExists(expectedPaths);

        String expectedAgentMind = """
                package my.project;
                
                import my.project.codelets.test.TestCodelet;
                import br.unicamp.cst.core.entities.Codelet;
                import br.unicamp.cst.core.entities.Memory;
                import br.unicamp.cst.core.entities.Mind;
                
                public class AgentMind extends Mind {
                
                    AgentMind() {
                        super();
                      \s
                        // Codelets Groups Declaration
                        createCodeletGroup("test");
                      \s
                        // Memory Groups Declaration
                        createMemoryGroup("test");
                      \s
                        Memory memOne;
                        Memory memTwo;
                        Memory memThree;
                      \s
                        memOne = createMemoryObject("MemOne");
                        registerMemory(memOne, "test");
                        memTwo = createMemoryContainer("MemTwo");
                        registerMemory(memTwo, "test");
                        memThree = createMemoryObject("MemThree");
                        registerMemory(memThree, "test");
                      \s
                        Codelet testCodelet = new TestCodelet();
                        testCodelet.addInput(memOne);
                        testCodelet.addOutput(memTwo);
                        testCodelet.addBroadcast(memThree);
                        insertCodelet(testCodelet);
                        registerCodelet(testCodelet, "test");
                      \s
                        for (Codelet c : this.getCodeRack().getAllCodelets()) {
                            c.setTimeStep(200);
                        }
                        start();
                    }
                }""";

        assertEquals(expectedAgentMind, readFileFromTmpDir("src/main/java/my/project/AgentMind.java"));
    }

    private File createMockYAMLFile() {
        // Create a mock YAML config file
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
    public void testIncorrectYAMLConfigParsing(){
        // Create a mock YAML config file
        String yamlConfig = """
                project-name: MyProject""";

        File configFile = new File(tempDir.toString(), "test_config.yaml");
        try {
            FileWriter writer = new FileWriter(configFile);
            writer.write(yamlConfig);
            writer.close();
        } catch (IOException e) {
            fail("Failed to create mock config file");
        }
        exitCode = new CommandLine(new Main()).execute("init", "--file", configFile.toString());
        assertEquals(1, exitCode);
        assertEquals("""
                Configuration File contains errors. Could not parse configurations.
                 in 'string', line 1, column 1:
                    project-name: MyProject
                    ^
                """,
                out.toString());
        List<String> expectedPaths = Arrays.asList(
                "/src/main/java/my/project/Main.java",
                "/src/main/java/my/project/AgentMind.java",
                "/src/main/java/my/project/codelets",
                "/src/main/java/my/project/codelets/test/TestCodelet.java"
        );
        assertPathsNotExists(expectedPaths);

    }

    @Test
    public void testProjectNameAndPackageOptions() throws IOException {
        exitCode = new CommandLine(new Main()).execute("init", "--project-name", "ProjectName", "--package", "project.name");
        assertEquals(0, exitCode);

        List<String> expectedPaths = Arrays.asList(
                "/src/main/java/project/name/Main.java",
                "/src/main/java/project/name/AgentMind.java",
                "/src/main/java/project/name/codelets"
        );
        assertPathsExists(expectedPaths);

        assertEquals("rootProject.name = 'ProjectName'", readFileFromTmpDir("/settings.gradle"));

    }

    @Test
    public void testGradleConfigFiles() throws IOException {
        initBasicTestProject();
        assertEquals(0, exitCode);

        String buildGradle = readFileFromTmpDir("/build.gradle");
        boolean isApplication = buildGradle.contains("id 'application'");
        boolean hasCSTDependency = buildGradle.contains("maven { url 'https://jitpack.io' }") &&
                                   buildGradle.contains("implementation 'com.github.CST-Group:cst:1.4.1'");
        boolean hasMainClassDefinition = buildGradle.contains("mainClass = 'testProject.Main'");
        assertTrue(isApplication);
        assertTrue(hasCSTDependency);
        assertTrue(hasMainClassDefinition);
    }

    @Test
    public void testCSTVersionOption() throws IOException {
        initBasicTestProject("--cst-version", "1.4.0");
        originalOut.println(out.toString());
        assertEquals(0, exitCode);

        List<String> expectedPaths = Arrays.asList(
                "/src/main/java/testProject/Main.java",
                "/src/main/java/testProject/AgentMind.java",
                "/src/main/java/testProject/codelets"
        );
        assertPathsExists(expectedPaths);

        String gradleSettings = readFileFromTmpDir("/build.gradle");
        boolean isPresent = gradleSettings.contains("com.github.CST-Group:cst:1.4.0");
        assertTrue(isPresent);
    }

    @Test
    public void testOverwriteOption() throws IOException {
        File mainFile = new File(tempDir.toString(), "src/main/java/testProject");
        mainFile.mkdirs();
        mainFile = new File(mainFile + "/Main.java");
        String mockText = "Overwrite this text";
        try {
            FileWriter writer = new FileWriter(mainFile);
            writer.write(mockText);
            writer.close();
        } catch (IOException e) {
            originalOut.println(e.toString());
            fail("Failed to create mock main file");
        }

        initBasicTestProject("--overwrite");
        assertEquals(0, exitCode);
        String modifiedFile = readFileFromTmpDir("src/main/java/testProject/Main.java");
        assertNotEquals(mockText, modifiedFile);
    }

    @Test
    public void testOverwriteOverAgentMind() throws IOException {
        File configFile = createMockYAMLFile();
        exitCode = new CommandLine(new Main()).execute("init", "--file", configFile.toString());
        assertEquals(0, exitCode);


        // Create a new mock YAML config file
        String yamlConfig = """
                projectName: MyProject
                packageName: my.project
                codelets:
                  - name: TestCodelet
                    group: test
                    in: [MemOne]
                    out: [MemTwo]
                    broadcast: [MemThree]
                  - name: NewCodelet
                    group: newGroup
                    in: [MemFour]
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
                    name: MemFour
                    type: object
                  - content: null
                    group: test
                    name: MemThree
                    type: object""";

        File newConfigFile = new File(tempDir.toString(), "new_config.yaml");
        try {
            FileWriter writer = new FileWriter(newConfigFile);
            writer.write(yamlConfig);
            writer.close();
        } catch (IOException e) {
            fail("Failed to create mock config file");
        }

        exitCode = new CommandLine(new Main()).execute("init", "--overwrite", "--file", newConfigFile.toString());
        assertEquals(0, exitCode);

        String expectedAgentMind = """
                package my.project;
                
                import my.project.codelets.test.TestCodelet;
                import my.project.codelets.newgroup.NewCodelet;
                import br.unicamp.cst.core.entities.Codelet;
                import br.unicamp.cst.core.entities.Memory;
                import br.unicamp.cst.core.entities.Mind;
                
                public class AgentMind extends Mind {
                
                    AgentMind() {
                        super();
                      \s
                        // Codelets Groups Declaration
                        createCodeletGroup("test");
                        createCodeletGroup("newGroup");
                      \s
                        // Memory Groups Declaration
                        createMemoryGroup("test");
                      \s
                        Memory memOne;
                        Memory memTwo;
                        Memory memFour;
                        Memory memThree;
                      \s
                        memOne = createMemoryObject("MemOne");
                        registerMemory(memOne, "test");
                        memTwo = createMemoryContainer("MemTwo");
                        registerMemory(memTwo, "test");
                        memFour = createMemoryObject("MemFour");
                        registerMemory(memFour, "test");
                        memThree = createMemoryObject("MemThree");
                        registerMemory(memThree, "test");
                      \s
                        Codelet testCodelet = new TestCodelet();
                        testCodelet.addInput(memOne);
                        testCodelet.addOutput(memTwo);
                        testCodelet.addBroadcast(memThree);
                        insertCodelet(testCodelet);
                        registerCodelet(testCodelet, "test");
                      \s
                        Codelet newCodelet = new NewCodelet();
                        newCodelet.addInput(memFour);
                        newCodelet.addOutput(memTwo);
                        newCodelet.addBroadcast(memThree);
                        insertCodelet(newCodelet);
                        registerCodelet(newCodelet, "newGroup");
                      \s
                        for (Codelet c : this.getCodeRack().getAllCodelets()) {
                            c.setTimeStep(200);
                        }
                        start();
                    }
                }""";

        assertEquals(expectedAgentMind, readFileFromTmpDir("src/main/java/my/project/AgentMind.java"));

        List<String> expectedPaths = Arrays.asList(
                "/src/main/java/my/project/Main.java",
                "/src/main/java/my/project/AgentMind.java",
                "/src/main/java/my/project/codelets",
                "/src/main/java/my/project/codelets/test/TestCodelet.java",
                "/src/main/java/my/project/codelets/newgroup/NewCodelet.java"
        );
        assertPathsExists(expectedPaths);
    }

    @Test
    public void testMemoryTypesInYAMLFile(){
        // Create a mock YAML config file
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
                    type: invalid
                  - content: null
                    group: test
                    name: MemTwo
                    type: container
                  - content: null
                    group: test
                    name: MemThree
                    type: object""";

        File configFile = new File(tempDir.toString(), "test_config.yaml");
        try {
            FileWriter writer = new FileWriter(configFile);
            writer.write(yamlConfig);
            writer.close();
        } catch (IOException e) {
            fail("Failed to create mock config file");
        }

        exitCode = new CommandLine(new Main()).execute("init", "--overwrite", "--file", configFile.toString());
        assertNotEquals(0, exitCode);
    }

    @Test
    public void testYAMLConfigWithCodeletNoGroup(){
        // Create a mock YAML config file
        String yamlConfig = """
                projectName: MyProject
                packageName: my.project
                codelets:
                  - name: TestCodelet
                    group:
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

        File configFile = new File(tempDir.toString(), "test_config.yaml");
        try {
            FileWriter writer = new FileWriter(configFile);
            writer.write(yamlConfig);
            writer.close();
        } catch (IOException e) {
            fail("Failed to create mock config file");
        }

        exitCode = new CommandLine(new Main()).execute("init", "--overwrite", "--file", configFile.toString());
        originalOut.println(out.toString());
        assertEquals(0, exitCode);

        assertPathsExists(Arrays.asList("/src/main/java/my/project/codelets/TestCodelet.java"));

    }

}
