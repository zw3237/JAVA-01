import java.lang.reflect.Method;

public class ClassLoaderHomeWorkDemo {
    public static void main(String[] args) {
        MyClassLoader myClassLoader = new MyClassLoader();
        try {
            Class<?> Log = myClassLoader.loadClass("Hello");
            Method method = Log.getMethod("hello");
            method.invoke(Log.newInstance());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
