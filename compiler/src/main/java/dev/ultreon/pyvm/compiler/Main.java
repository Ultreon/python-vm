package dev.ultreon.pyvm.compiler;

import dev.ultreon.pyvm.compiler.ast.PyFileNode;
import dev.ultreon.pyvm.compiler.parser.PythonLexer;
import dev.ultreon.pyvm.compiler.parser.PythonParser;
import dev.ultreon.pyvm.compiler.util.PythonErrorListener;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main {
    private static boolean verbose;
    private static String inputFileName;

    public static void main(String[] args) {
        OptionParser parser = new OptionParser();

        OptionSpec<File> inputOpt = parser.accepts("input")
                .withRequiredArg()
                .required()
                .ofType(File.class)
                .describedAs("input file");

        OptionSpec<File> outputOpt = parser.accepts("output")
                .withRequiredArg()
                .required()
                .ofType(File.class)
                .describedAs("output class file");

        OptionSpec<String> packageOpt = parser.accepts("package")
                .withRequiredArg()
                .ofType(String.class)
                .describedAs("package name");

        OptionSpec<Void> verboseOpt = parser.acceptsAll(List.of("v", "verbose"), "enable verbose output");
        OptionSpec<Void> helpOpt = parser.acceptsAll(List.of("h", "help", "?"), "show help");

        try {
            OptionSet options = parser.parse(args);

            if (options.has(helpOpt)) {
                parser.printHelpOn(System.out);
                System.exit(0);
            }

            Path inputFile = options.valueOf(inputOpt).toPath();
            Path outputFile = options.valueOf(outputOpt).toPath();
            String packageName = options.has(packageOpt) ? options.valueOf(packageOpt) : "pyvm";
            verbose = options.has(verboseOpt);
            inputFileName = inputFile.getFileName().toString();

            verbose("Compiling " + inputFile + " to " + outputFile);

            String classname = getFileClassname();
            CharStream cs = CharStreams.fromStream(Files.newInputStream(inputFile));
            PythonLexer pyLexer = new PythonLexer(cs);
            CommonTokenStream tokens = new CommonTokenStream(pyLexer);
            PythonParser pyParser = new PythonParser(tokens);
            pyParser.addErrorListener(new PythonErrorListener());
            PythonParser.File_inputContext root = pyParser.file_input();

            ClassNode cn = new ClassNode();
            cn.version = Opcodes.V1_8;
            cn.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER;
            cn.superName = "java/lang/Object";
            cn.sourceFile = inputFileName;

            Compiler compiler = new Compiler(packageName + "." + classname, cn.sourceFile, outputFile);
            PyFileNode pyFileNode = compiler.visitFile_input(root);
            pyFileNode.writeClass(outputFile, compiler.getContext());
            compiler.finish();

            if (!packageName.isEmpty())
                Files.createDirectories(outputFile = outputFile.resolve(packageName.replace('.', '/')));

            if (Files.notExists(outputFile)) Files.createDirectories(outputFile);

            // TODO: Handle input/output files
        } catch (OptionException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println();
            try {
                parser.printHelpOn(System.err);
            } catch (IOException ex) {
                // Ignore
            }
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static String getFileClassname() {
        String rawName = getRawName();
        rawName = Character.toUpperCase(rawName.charAt(0)) + rawName.substring(1);
        return rawName;
    }

    private static String getRawName() {
        return inputFileName.substring(0, inputFileName.lastIndexOf('.'));
    }

    public static void verbose(String message) {
        if (verbose) System.out.println(message);
    }

    public static void writeBytesToFile(Path resolve, byte[] bytes) {
        System.out.println("Info: Writing class file to " + resolve);

        if (Files.notExists(resolve.getParent())) {
            try {
                Files.createDirectories(resolve.getParent());
            } catch (IOException e) {
                System.err.println("Error: Failed to create directory " + resolve.getParent());
                e.printStackTrace();
                return;
            }
        }

        try {
            Files.write(resolve, bytes, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("Error: Failed to write class file to " + resolve);
            e.printStackTrace();
        }
    }

    public static void writeClassToFile(Path resolve, ClassNode classNode) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        CheckClassAdapter adapter = new CheckClassAdapter(writer);
        classNode.accept(adapter);
        writeBytesToFile(resolve, writer.toByteArray());
    }
}