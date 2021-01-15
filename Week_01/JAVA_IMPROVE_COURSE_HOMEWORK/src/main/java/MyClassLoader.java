import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class MyClassLoader extends ClassLoader {
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class log = null;
        //获取该文件的字节码数组
        byte[] classData = null;
        try {
            String path = "/Users/zhengwei/IdeaProjects/java-imporve-course/JAVA_IMPROVE_COURSE_HOMEWORK/src/main/resources/hello/Hello.xlass";
            classData = this.getContent(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] bytes = new byte[classData.length];
        for (int i = 0; i < classData.length; i++) {
            bytes[i] = (byte) (255 - classData[i]);
        }
        log = defineClass(name, bytes, 0, bytes.length);
        return log;

    }

    public byte[] getContent(String filePath) throws IOException {
        File file = new File(filePath);
        long fileSize = file.length();
        FileInputStream fi = new FileInputStream(file);
        byte[] buffer = new byte[(int) fileSize];
        int offset = 0;
        int numRead = 0;
        while (offset < buffer.length
                && (numRead = fi.read(buffer, offset, buffer.length - offset)) >= 0) {
            offset += numRead;
        }
        fi.close();
        return buffer;
    }

}