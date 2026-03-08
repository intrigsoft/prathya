package dev.prathya.core.scanner;

import dev.prathya.core.model.TraceEntry;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Scans compiled test class directories by reading {@code .class} file bytecode directly,
 * without loading classes via a ClassLoader. This avoids classpath issues when project
 * dependencies (Spring, JPA, etc.) are not available — making it suitable for the MCP server.
 */
public class BytecodeAnnotationScanner implements AnnotationScanner {

    private static final String REQUIREMENT_DESC = "Ldev/prathya/annotations/Requirement;";
    private static final String REQUIREMENT_LIST_DESC = "Ldev/prathya/annotations/Requirement$List;";

    @Override
    public List<TraceEntry> scan(List<Path> testClassDirectories) {
        return scan(testClassDirectories, List.of());
    }

    @Override
    public List<TraceEntry> scan(List<Path> testClassDirectories, List<Path> additionalClasspath) {
        List<TraceEntry> entries = new ArrayList<>();
        for (Path dir : testClassDirectories) {
            if (!Files.isDirectory(dir)) continue;
            try {
                Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (file.getFileName().toString().endsWith(".class")) {
                            String className = dir.relativize(file).toString()
                                    .replace(file.getFileSystem().getSeparator(), ".")
                                    .replaceAll("\\.class$", "");
                            scanClassFile(file, className, entries);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException("Failed to walk directory: " + dir, e);
            }
        }
        return entries;
    }

    private void scanClassFile(Path file, String className, List<TraceEntry> entries) {
        try {
            byte[] data = Files.readAllBytes(file);

            // Quick check: skip files that don't contain the annotation descriptor
            if (!containsBytes(data, REQUIREMENT_DESC) && !containsBytes(data, REQUIREMENT_LIST_DESC)) {
                return;
            }

            parseClassFile(data, className, entries);
        } catch (Exception e) {
            // skip unparseable files
        }
    }

    private boolean containsBytes(byte[] data, String text) {
        byte[] needle = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        outer:
        for (int i = 0; i <= data.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (data[i + j] != needle[j]) continue outer;
            }
            return true;
        }
        return false;
    }

    private void parseClassFile(byte[] data, String className, List<TraceEntry> entries) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));

        int magic = dis.readInt();
        if (magic != 0xCAFEBABE) return;

        dis.readUnsignedShort(); // minor version
        dis.readUnsignedShort(); // major version

        // Parse constant pool
        int cpCount = dis.readUnsignedShort();
        String[] utf8Pool = readConstantPool(dis, cpCount);

        dis.readUnsignedShort(); // access_flags
        dis.readUnsignedShort(); // this_class
        dis.readUnsignedShort(); // super_class

        // Skip interfaces
        int interfaceCount = dis.readUnsignedShort();
        for (int i = 0; i < interfaceCount; i++) dis.readUnsignedShort();

        // Skip fields
        int fieldCount = dis.readUnsignedShort();
        for (int i = 0; i < fieldCount; i++) {
            dis.readUnsignedShort(); // access_flags
            dis.readUnsignedShort(); // name_index
            dis.readUnsignedShort(); // descriptor_index
            skipAttributes(dis);
        }

        // Parse methods
        int methodCount = dis.readUnsignedShort();
        for (int i = 0; i < methodCount; i++) {
            dis.readUnsignedShort(); // access_flags
            int nameIndex = dis.readUnsignedShort();
            dis.readUnsignedShort(); // descriptor_index
            String methodName = utf8Pool[nameIndex];

            List<String> reqIds = new ArrayList<>();
            int attrCount = dis.readUnsignedShort();
            for (int a = 0; a < attrCount; a++) {
                int attrNameIndex = dis.readUnsignedShort();
                int attrLength = dis.readInt();
                String attrName = utf8Pool[attrNameIndex];

                if ("RuntimeVisibleAnnotations".equals(attrName)) {
                    parseRuntimeAnnotations(dis, utf8Pool, reqIds);
                } else {
                    dis.skipBytes(attrLength);
                }
            }

            if (!reqIds.isEmpty() && methodName != null) {
                entries.add(new TraceEntry(className, methodName,
                        reqIds.stream().distinct().collect(Collectors.toList())));
            }
        }
    }

    /**
     * Reads the constant pool and returns an array where index i holds the UTF-8 string
     * for CONSTANT_Utf8 entries (null for other types).
     */
    private String[] readConstantPool(DataInputStream dis, int cpCount) throws IOException {
        String[] pool = new String[cpCount];
        for (int i = 1; i < cpCount; i++) {
            int tag = dis.readUnsignedByte();
            switch (tag) {
                case 1: // CONSTANT_Utf8
                    pool[i] = dis.readUTF();
                    break;
                case 3: // CONSTANT_Integer
                case 4: // CONSTANT_Float
                    dis.skipBytes(4);
                    break;
                case 5: // CONSTANT_Long
                case 6: // CONSTANT_Double
                    dis.skipBytes(8);
                    i++; // takes two slots
                    break;
                case 7: // CONSTANT_Class
                case 8: // CONSTANT_String
                case 16: // CONSTANT_MethodType
                case 19: // CONSTANT_Module
                case 20: // CONSTANT_Package
                    dis.skipBytes(2);
                    break;
                case 9: // CONSTANT_Fieldref
                case 10: // CONSTANT_Methodref
                case 11: // CONSTANT_InterfaceMethodref
                case 12: // CONSTANT_NameAndType
                case 17: // CONSTANT_Dynamic
                case 18: // CONSTANT_InvokeDynamic
                    dis.skipBytes(4);
                    break;
                case 15: // CONSTANT_MethodHandle
                    dis.skipBytes(3);
                    break;
                default:
                    throw new IOException("Unknown constant pool tag: " + tag + " at index " + i);
            }
        }
        return pool;
    }

    private void skipAttributes(DataInputStream dis) throws IOException {
        int count = dis.readUnsignedShort();
        for (int i = 0; i < count; i++) {
            dis.readUnsignedShort(); // name_index
            int length = dis.readInt();
            dis.skipBytes(length);
        }
    }

    private void parseRuntimeAnnotations(DataInputStream dis, String[] pool, List<String> reqIds) throws IOException {
        int numAnnotations = dis.readUnsignedShort();
        for (int i = 0; i < numAnnotations; i++) {
            parseAnnotation(dis, pool, reqIds);
        }
    }

    private void parseAnnotation(DataInputStream dis, String[] pool, List<String> reqIds) throws IOException {
        int typeIndex = dis.readUnsignedShort();
        String typeDesc = pool[typeIndex];
        int numPairs = dis.readUnsignedShort();

        boolean isRequirement = REQUIREMENT_DESC.equals(typeDesc);
        boolean isRequirementList = REQUIREMENT_LIST_DESC.equals(typeDesc);

        for (int p = 0; p < numPairs; p++) {
            int nameIndex = dis.readUnsignedShort();
            String elementName = pool[nameIndex];

            if ((isRequirement || isRequirementList) && "value".equals(elementName)) {
                collectElementValue(dis, pool, reqIds);
            } else {
                skipElementValue(dis);
            }
        }
    }

    /**
     * Reads an element_value and collects any string values into reqIds.
     * For arrays, recursively collects from each element.
     * For nested annotations (@Requirement inside @Requirement.List), recursively parses.
     */
    private void collectElementValue(DataInputStream dis, String[] pool, List<String> reqIds) throws IOException {
        int tag = dis.readUnsignedByte();
        switch (tag) {
            case 's': // String
                int stringIndex = dis.readUnsignedShort();
                if (pool[stringIndex] != null) {
                    reqIds.add(pool[stringIndex]);
                }
                break;
            case '[': // Array
                int numValues = dis.readUnsignedShort();
                for (int i = 0; i < numValues; i++) {
                    collectElementValue(dis, pool, reqIds);
                }
                break;
            case '@': // Nested annotation
                parseAnnotation(dis, pool, reqIds);
                break;
            default:
                skipElementValueBody(tag, dis);
                break;
        }
    }

    private void skipElementValue(DataInputStream dis) throws IOException {
        int tag = dis.readUnsignedByte();
        skipElementValueBody(tag, dis);
    }

    private void skipElementValueBody(int tag, DataInputStream dis) throws IOException {
        switch (tag) {
            case 'B': case 'C': case 'D': case 'F':
            case 'I': case 'J': case 'S': case 'Z':
            case 's': // const_value_index
                dis.skipBytes(2);
                break;
            case 'e': // enum_const_value
                dis.skipBytes(4);
                break;
            case 'c': // class_info_index
                dis.skipBytes(2);
                break;
            case '@': // annotation_value
                skipAnnotation(dis);
                break;
            case '[': // array_value
                int numValues = dis.readUnsignedShort();
                for (int i = 0; i < numValues; i++) {
                    skipElementValue(dis);
                }
                break;
        }
    }

    private void skipAnnotation(DataInputStream dis) throws IOException {
        dis.readUnsignedShort(); // type_index
        int numPairs = dis.readUnsignedShort();
        for (int i = 0; i < numPairs; i++) {
            dis.readUnsignedShort(); // element_name_index
            skipElementValue(dis);
        }
    }
}
