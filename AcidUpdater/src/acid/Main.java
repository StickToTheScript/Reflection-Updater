package acid;

import acid.other.ClassAnalyser;
import acid.other.Finder;
import acid.other.InstructionPrinter;
import acid.other.JarParser;
import acid.structures.ClassInfo;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.TypePath;
import jdk.internal.org.objectweb.asm.tree.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Created by Kira on 2014-12-06.
 */
public class Main {

    static ClassAnalyser analyser = null;

    public static void main(String[] args) {
        String url = System.getEnv("GAME_WORLD_URL");
        analyser = new ClassAnalyser(url, String.format("%d.jar", getLatestRevision(url,202)), true);
//        analyser = new ClassAnalyser(url, String.format("%d.jar", 202), true);
        //analyser = new ClassAnalyser(url, String.format("%s.jar", "Android"), true);
        analyser.analyse();
        //analyser.print();
        analyser.printSimbaNative();
        //analyser.refactor(String.format("Refactor_%d.jar", getLatestRevision(191)));

//
//        System.out.println("COUNT: " + countInstructionsToField("bb", "<clinit>", "aj", Opcodes.PUTSTATIC
    }

    private static int getLatestRevision(String url, int currentVersion) {
        url = url.replace("http://", "");
        for (int i = 0; i < 100; ++i) {
            try {
                Socket socket = new Socket(url, 43594);
                ByteBuffer packet = ByteBuffer.allocate(43);
                packet.put((byte) 15); //Handshake
                packet.putInt(currentVersion);
                packet.putInt(0);


                OutputStream outputStream = socket.getOutputStream();
//                outputStream.write(packet.array());
                outputStream.write(new byte[]{15, 0, 0, (byte) (currentVersion >> 8), (byte) currentVersion});
                outputStream.flush();

                if (socket.getInputStream().read() != 6) {
                    socket.close();
                    return currentVersion;
                }

                socket.close();

                ++currentVersion;
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }

        return -1;
    }

    public static int countInstructionsToField(String cls, String method, String field, int code) {
        MethodNode[] methods = getClass(cls).methods.stream().filter(m -> m.name.equals(method)).toArray((int count) -> new MethodNode[count]);

        for (MethodNode m : methods) {
            int count = 0;
            int start = 0;

            while (true) {
                start = new Finder(m).findNext(start, code, false);

                if (start != -1) {
                    if (m.instructions.get(start).getOpcode() == code) {
                        FieldInsnNode f = (FieldInsnNode)m.instructions.get(start);
                        if (f.name.equals(field)) {
                            return count;
                        }
                    }
                    ++count;
                    ++start;
                }
                else {
                    break;
                }
            }
            return -1;
        }

        return -1;
    }

    private static void printMethod(String cls, String method) {
        getClasses().stream().filter(c -> c.name.equals(cls)).forEach(c -> c.methods.stream().filter(m -> m.name.equals(method)).forEach(m -> new InstructionPrinter(m)));
    }

    public static String get(String name) {
        return analyser.getClassName(name);
    }

    public static ClassInfo getInfo(String name) {
        return analyser.getInfo(name);
    }

    public static ClassNode getClass(String name) {
       return analyser.getClass(name);
    }

    public static ClassNode getClassNode(String name) {
        return analyser.getClassNode(name);
    }

    public static Collection<ClassNode> getClasses() {
        return analyser.getClasses();
    }

    public static void findField(String className, String field) {
        analyser.findField(className, field);
    }

    public static void findMethod(String className, String superName, String name, String desc) {
        analyser.findMethod(className, superName, name, desc);
    }

    public static void findMethodUsage(String className, String name, String desc) {
        analyser.findMethodUsage(className, name, desc);
    }

    public static long findMultiplier(String owner, String field) {
        return analyser.findMultiplier(owner, field);
    }
}
