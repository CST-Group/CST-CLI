package commands;

import br.unicamp.cst.cli.data.MemoryConfig;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MemoryConfigTest {

    @Test
    public void testMemoryConfigCreation(){
        MemoryConfig testObj = new MemoryConfig();
        testObj.setName("TestObj");
        testObj.setType("object");
        testObj.setGroup("TestGroup");
        testObj.setContent(Map.of("int", 1));

        assertEquals("TestObj", testObj.getName());
        assertEquals("object", testObj.getType());
        assertEquals("TestGroup", testObj.getGroup());
        assertEquals(Map.of("int", 1), testObj.getContent());
    }

    @Test
    public void testSetContentChecks(){
        MemoryConfig testObj = new MemoryConfig();

        //Correct assignments
        testObj.setContent(Map.of("int", 1));
        assertEquals(Map.of("int", 1), testObj.getContent());
        testObj.setContent(Map.of("double", 1));
        assertEquals(Map.of("double", 1), testObj.getContent());
        testObj.setContent(Map.of("String", 1));
        assertEquals(Map.of("String", 1), testObj.getContent());
        testObj.setContent(Map.of("char", 'a'));
        assertEquals(Map.of("char", 'a'), testObj.getContent());
        testObj.setContent(Map.of("float", 1));
        assertEquals(Map.of("float", 1), testObj.getContent());

        //Not a basic type
        assertThrows(YAMLException.class, () -> testObj.setContent(Map.of("List",1)));

        //More than one content
        assertThrows(YAMLException.class, () -> testObj.setContent(Map.of("int",1, "float", 2)));

        //Null content
        testObj.setContent(null);
        assertNull(testObj.getContent());
    }

    @Test
    public void testSetTypeChecks(){
        MemoryConfig testObj = new MemoryConfig();

        //Correct assignments
        testObj.setType("object");
        assertEquals("object", testObj.getType());
        testObj.setType("oBjEcT");
        assertEquals("object", testObj.getType());
        testObj.setType("container");
        assertEquals("container", testObj.getType());
        testObj.setType("CoNtAiNeR");
        assertEquals("container", testObj.getType());

        //Not valid type
        assertThrows(YAMLException.class, () -> testObj.setType("List"));

        //Null type
        assertThrows(YAMLException.class, () -> testObj.setType(null));
    }
}
