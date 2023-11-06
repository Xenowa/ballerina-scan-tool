package balToolTester;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.tools.text.LineRange;
import org.wso2.ballerinalang.compiler.tree.BLangFunction;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Main {
    private static PrintStream outputStream = System.out;
    private static PrintStream errorStream = System.err;

    public static void main(String[] args) {
        // Set a list of paths to check for the jar installation in the users system
        String[] sonarBallerinaJARLocations = new String[]{
          // We will consider windows only paths for now
          "C:/Program Files/sonarqube-9.9.2.77730/extensions/plugins",
          "C:/src/sonarqube-9.9.2.77730/extensions/plugins",
        };

        // Iterate through the list and find the first location where the jar can be found
        File jarFile = null;
        for(String pluginLocation : sonarBallerinaJARLocations){
            jarFile = new File(pluginLocation + "/" + "sonar-ballerina-plugin-1.0-SNAPSHOT-all.jar");
            if(jarFile.exists()){
                outputStream.println("Using sonar ballerina plugin from: " + pluginLocation);
                break;
            }else{
                jarFile = null;
            }
        }

        // TODO: if the jar file is not found, attempt to install the sonar ballerina plugin in the users sonarqube installation directory
        if(jarFile == null){
            outputStream.println("sonar ballerina plugin not found!");
            // for now we will terminate the command execution
            return;
        }

        // Create a process that executes the jar file
        ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", jarFile.getAbsolutePath());

        // start the process
        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // create an input stream reader
        InputStream io = process.getInputStream();
        InputStreamReader ioReader = new InputStreamReader(io);
        BufferedReader pluginOutputStream = new BufferedReader(ioReader);

        // Read the outputs of the jar file execution
        String line;
        try{
            while((line = pluginOutputStream.readLine()) != null){
                outputStream.println(line);
            }
        }catch (IOException e){
            throw new RuntimeException(e);
        }

        // create an error stream reader
        InputStream errorIO = process.getInputStream();
        InputStreamReader errorIOReader = new InputStreamReader(errorIO);
        BufferedReader pluginErrorOutputStream = new BufferedReader(errorIOReader);

        // Read the outputs of the jar file execution
        try{
            while((line = pluginErrorOutputStream.readLine()) != null){
                outputStream.println(line);
            }
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }
}