package com.pojson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
class PoJsonService {
    public static ObjectMapper JACKSON = new ObjectMapper();
    private final Logger logger = LoggerFactory.getLogger(PoJsonService.class);

    public byte[] generateFile(String json, String packageName) throws IOException {
        JsonNode rootNode = JACKSON.readTree(json);
        Map<String, String> generatedClasses = new HashMap<>(); // map with all java class files
        generateJavaFiles("RootNode", rootNode, generatedClasses);
        logger.info("Java classes created! Saving to a file");
        return createZipFile(generatedClasses, packageName);
    }

    private void generateJavaFiles(String className, JsonNode jsonNode, Map<String, String> generatedClasses) {
        if (generatedClasses.containsKey(className)) {
            return;
        }

        StringBuilder content = new StringBuilder(); // contains all the java class file content(fields, getters, setters)
        content.append("public class ").append(className)
                .append(" {").append(System.lineSeparator());

        Map<String, String> fields = new LinkedHashMap<>(); // to store fields & fieldTypes to create getters/setters
        Iterator<String> fieldIterator = jsonNode.fieldNames();

        // add all private fields
        while (fieldIterator.hasNext()) {
            String field = fieldIterator.next();
            JsonNode fieldNode = jsonNode.get(field);

            String fieldType = determineFieldType(fieldNode, field, generatedClasses);

            fields.put(field, fieldType);
            content.append("\tprivate ").append(fieldType).append(" ").append(field).append(";\n");
        }

        // add getters & setters
        for (Map.Entry<String, String> fieldEntry : fields.entrySet()) {
            String setter = createSetters(fieldEntry.getKey(), fieldEntry.getValue());
            String getter = createGetters(fieldEntry.getKey(), fieldEntry.getValue());
            content.append(setter).append(getter);
        }


        content.append("}\n");
        // add java class content to the map
        generatedClasses.put(className, content.toString());
    }


    private byte[] createZipFile(Map<String, String> classes, String packageName) throws IOException {
        logger.info("Creating a zip file with POJO classes");
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutStream = new ZipOutputStream(outStream)) {
            // iterate each class and write to a separate file in zip
            for (Map.Entry<String, String> clazzContent : classes.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(clazzContent.getKey() + ".java");
                zipOutStream.putNextEntry(zipEntry);

                String packages = createPackageName(packageName);
                String imports = createImports(clazzContent.getValue());
                String content = packages + imports + clazzContent.getValue();
                zipOutStream.write(content.getBytes(StandardCharsets.UTF_8));
                zipOutStream.closeEntry();
            }
        }
        logger.info("Created a zip file with classes: {}", classes.keySet());
        return outStream.toByteArray();
    }

    private String createPackageName(String packageName) {
        if (isValidPackageName(packageName)) {
            return "package " + packageName + ";" + System.lineSeparator() + System.lineSeparator();
        }
        return "// add your package here" + System.lineSeparator() + System.lineSeparator();
    }

    private String determineFieldType(JsonNode jsonNode, String fieldName, Map<String, String> generatedClasses) {
        if (jsonNode.isTextual()) {
            return "String";
        } else if (jsonNode.isBoolean()) {
            return "Boolean";
        } else if (jsonNode.isInt()) {
            return "Integer";
        } else if (jsonNode.isDouble()) {
            return "Double";
        } else if (jsonNode.isLong()) {
            return "Long";
        } else if (jsonNode.isArray()) {
            List<JsonNode> arrayNode = new ArrayList<>();
            for (JsonNode node : jsonNode) {
                arrayNode.add(node);
            }
            JsonNode maxNode = arrayNode.stream()
                    .max(Comparator.comparingInt(JsonNode::size))
                    .orElse(null);
            if (maxNode == null) {
                return "List<Object>";
            }
            String elementType = determineFieldType(maxNode, fieldName, generatedClasses);
            return "List<" + elementType + ">";
        } else if (jsonNode.isObject()) {
            String nestedClassName = capitalizeFirstLetter(fieldName);
            generateJavaFiles(nestedClassName, jsonNode, generatedClasses);
            return nestedClassName;
        } else {
            return "Object";
        }
    }

    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }


    private boolean isValidPackageName(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }
        String[] parts = packageName.split("\\.");
        for (String part : parts) {
            if (part.isEmpty() || Character.isDigit(part.charAt(0)) || !part.matches("[a-zA-Z0-9]+")) {
                return false;
            }
        }
        logger.info("Package {} is valid", packageName);
        return true;
    }

    private String createGetters(String fieldName, String fieldType) {
        return String.format("\n\tpublic %s get%s() {\n\t\treturn %s;\n\t}\n",
                fieldType, capitalizeFirstLetter(fieldName), fieldName);
    }

    private String createSetters(String fieldName, String fieldType) {
        return String.format("\n\tpublic void set%s(%s %s) {\n\t\tthis.%s = %s;\n\t}\n",
                capitalizeFirstLetter(fieldName), fieldType, fieldName, fieldName, fieldName);
    }

    private String createImports(String content) {
        StringBuilder imports = new StringBuilder();
        if (content.contains("List")) {
            imports.append("import java.util.List;").append(System.lineSeparator());
        }
        if (content.contains("Map")) {
            imports.append("import java.util.Map;").append(System.lineSeparator());
        }
        if (imports.length() > 0) {
            imports.append(System.lineSeparator());
        }
        return imports.toString();
    }
}