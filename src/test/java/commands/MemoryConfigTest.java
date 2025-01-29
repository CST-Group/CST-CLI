package commands;

import br.unicamp.cst.cli.data.MemoryConfig;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

public class MemoryConfigTest {

    @Test
    public void testMemoryConfigCreation(){
        MemoryConfig testObj = new MemoryConfig();
        testObj.setName("TestObj");
        testObj.setType("object");
        testObj.setGroup("TestGroup");
        testObj.setContent(Map.of("int", 1));

        assertThat(testObj.getName()).isEqualTo("TestObj");
        assertThat(testObj.getType()).isEqualTo("object");
        assertThat(testObj.getGroup()).isEqualTo("TestGroup");
        assertThat(testObj.getContent()).containsExactly(entry("int", 1));
    }

    @Test
    public void testSetContentChecks(){
        MemoryConfig testObj = new MemoryConfig();

        //Correct assignments
        testObj.setContent(Map.of("int", 1));
        assertThat(testObj.getContent()).containsExactly(entry("int", 1));
        testObj.setContent(Map.of("double", 1));
        assertThat(testObj.getContent()).containsExactly(entry("double", 1));
        testObj.setContent(Map.of("String", 1));
        assertThat(testObj.getContent()).containsExactly(entry("String", 1));
        testObj.setContent(Map.of("char", 'a'));
        assertThat(testObj.getContent()).containsExactly(entry("char", 'a'));
        testObj.setContent(Map.of("float", 1));
        assertThat(testObj.getContent()).containsExactly(entry("float", 1));

        //Not a basic type
        assertThatThrownBy(() -> testObj.setContent(Map.of("List",1))).isInstanceOf(YAMLException.class);

        //More than one content
        assertThatThrownBy(() -> testObj.setContent(Map.of("int",1, "float", 2))).isInstanceOf(YAMLException.class);

        //Null content
        testObj.setContent(null);
        assertThat(testObj.getContent()).isNull();
    }

    @Test
    public void testSetTypeChecks(){
        MemoryConfig testObj = new MemoryConfig();

        //Correct assignments
        testObj.setType("object");
        assertThat(testObj.getType()).isEqualTo("object");
        testObj.setType("oBjEcT");
        assertThat(testObj.getType()).isEqualTo("object");
        testObj.setType("container");
        assertThat(testObj.getType()).isEqualTo("container");
        testObj.setType("CoNtAiNeR");
        assertThat(testObj.getType()).isEqualTo("container");

        //Not valid type
        assertThatThrownBy(() -> testObj.setType("List")).isInstanceOf(YAMLException.class);

        //Null type
        assertThatThrownBy(() -> testObj.setType(null)).isInstanceOf(YAMLException.class);
    }
}
