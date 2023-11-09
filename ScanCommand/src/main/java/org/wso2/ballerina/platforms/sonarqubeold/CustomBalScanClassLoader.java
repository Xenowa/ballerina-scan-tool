package org.wso2.ballerina.platforms.sonarqubeold;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class CustomBalScanClassLoader extends ClassLoader {
    private JarFile outerJarFile;
    private String innerJarName;

    public CustomBalScanClassLoader(String outerJarPath, String innerJarName) throws IOException {
        // Initiate the class loader with existing system classloader properties
        super(ClassLoader.getSystemClassLoader());

        // Custom Initializing properties of class loader
        // Setting the jar file location of the jar containing the nested jar
        this.outerJarFile = new JarFile(outerJarPath);
        this.innerJarName = innerJarName;
    }

    @Override
    protected Class<?> findClass(String nestedJarClassName) {
        // Use the custom class loader
        Class<?> c = null;
        try {
            // Retrieve the inner jar file from the outer jar
            JarEntry innerJarEntry = outerJarFile.getJarEntry(innerJarName);

            // Check if there is an inner jar first
            if (innerJarEntry != null) {
                // get the input stream of the inner jar to the outer jar
                try (InputStream inputStream = outerJarFile.getInputStream(innerJarEntry)) {
                    // Create a temporary file and write the contents of the InputStream to it
                    File tempFile = File.createTempFile("innerJarTemp", ".jar");

                    // Replace exisiting temporary jar if exist
                    Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    // Load the class from the inner jar file
                    try (JarFile innerJarFile = new JarFile(tempFile)) {
                        // format the user provided class name with the .class extension
                        JarEntry classEntry = innerJarFile.getJarEntry(nestedJarClassName.replace('.', '/') + ".class");
                        if (classEntry != null) {
                            // get the input stream of the class entry of the extracted jar
                            try (InputStream classInputStream = innerJarFile.getInputStream(classEntry)) {
                                // Retrieve the class code
                                byte[] classData = toByteArray(classInputStream);

                                // return the final loaded class
                                c = defineClass(nestedJarClassName, classData, 0, classData.length);
                            }
                        }
                    }

                    // Delete the temporary file
                    tempFile.delete();
                }
            }

            return c;
        } catch (Exception runtimeException) {
            throw new RuntimeException(runtimeException);
        }
    }

    // Custom class loading logic
    public Class<?> loadClass(String nestedJarClassName) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(nestedJarClassName)) {
            // First, check if the class has already been loaded
            Class<?> c = findLoadedClass(nestedJarClassName);
            return c;
        }
    }

    private byte[] toByteArray(InputStream classInputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int read;
        byte[] buffer = new byte[1024];
        while ((read = classInputStream.read(buffer, 0, buffer.length)) != -1) {
            byteArrayOutputStream.write(buffer, 0, read);
        }
        byteArrayOutputStream.flush();
        return byteArrayOutputStream.toByteArray();
    }
}
