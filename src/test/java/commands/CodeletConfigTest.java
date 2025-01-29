package commands;

import br.unicamp.cst.cli.data.CodeletConfig;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;


public class CodeletConfigTest {

    @Test
    public void testCodeletConfigCreation(){
        CodeletConfig testConfig = new CodeletConfig();
        testConfig.setName("TestConfig");
        testConfig.setGroup("TestGroup");
        testConfig.setIn(Arrays.asList("MemOne"));
        testConfig.setOut(Arrays.asList("MemTwo"));
        testConfig.setBroadcast(Arrays.asList("MemThree"));

        assertThat(testConfig.getName()).isEqualTo("TestConfig");
        assertThat(testConfig.getGroup()).isEqualTo("TestGroup");
        assertThat(testConfig.getIn()).containsOnly("MemOne");
        assertThat(testConfig.getOut()).containsOnly("MemTwo");
        assertThat(testConfig.getBroadcast()).containsOnly("MemThree");
    }

}
