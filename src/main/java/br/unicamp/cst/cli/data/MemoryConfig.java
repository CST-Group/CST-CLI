package br.unicamp.cst.cli.data;

import org.yaml.snakeyaml.error.YAMLException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static br.unicamp.cst.cli.util.CodeUtils.isValidName;


public class MemoryConfig {
    public static final String OBJECT_TYPE = "object";
    public static final String CONTAINER_TYPE = "container";
    public static final List<String> VALID_CONTENT_TYPES = Arrays.asList("int", "Integer", "boolean", "Boolean", "char", "Character", "short", "Short", "long", "Long", "float", "Float", "double", "Double", "byte", "Byte", "String");
    private String name;
    private String type;
    private Map<String, Object> content;
    private String group;

    public MemoryConfig(){}

    public MemoryConfig(String name){
        if (!isValidName(name)){
            System.err.println("MEMORY[" + name + "]: Invalid memory name. Name should be a valid Java identifier.");
            throw new YAMLException("MEMORY[" + name + "]: Invalid memory name. Name should be a valid Java identifier.");
        } else {
            this.setName(name);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (!isValidName(name)){
            System.err.println("MEMORY[" + name + "]: Invalid memory name. Name should be a valid Java identifier.");
            throw new YAMLException("MEMORY[" + name + "]: Invalid memory name. Name should be a valid Java identifier.");
        } else {
            this.name = name;
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        if (type != null) {
            if (OBJECT_TYPE.equals(type.toLowerCase()) || CONTAINER_TYPE.equals(type.toLowerCase())) {
                this.type = type.toLowerCase();
            } else {
                System.err.println("MEMORY[" + this.getName() + "]: Memory type should be 'object' or 'container'");
                throw new YAMLException("MEMORY[" + this.getName() + "]: Memory type should be 'object' or 'container'");
            }
        } else {
            System.err.println("MEMORY[" + this.getName() + "]: Memory type must be specified!");
            throw new YAMLException("MEMORY[" + this.getName() + "]: Memory type must be specified!");
        }
    }

    public Map<String, Object> getContent() {
        return content;
    }

    public void setContent(Map<String,Object> content) {
        if (content != null) {
            if (content.size() > 1) {
                System.err.println("MEMORY[" + this.getName() + "]: Memory content should have at most one item");
                throw new YAMLException("MEMORY[" + this.getName() + "]: Memory content should have at most one item");
            }
            if (content.size() == 1) {
                String contentType = content.keySet().iterator().next();
                if (!VALID_CONTENT_TYPES.contains(contentType)) {
                    System.err.println("MEMORY[" + this.getName() + "]: Invalid memory content type!");
                    throw new YAMLException("MEMORY[" + this.getName() + "]: Invalid memory content type!");
                }
            }
        }
        this.content = content;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Override
    public String toString() {
        return "MemoryConfig{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", content='" + content + '\'' +
                ", group='" + group + '\'' +
                '}';
    }
}
