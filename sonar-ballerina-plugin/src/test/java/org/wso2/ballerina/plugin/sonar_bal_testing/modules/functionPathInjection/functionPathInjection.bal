import ballerina/file;
import ballerina/http;
import ballerina/io;

listener http:Listener endpoint = new (8080);
string targetDirectory = "./modules/functionPathInjection/";

service / on endpoint {
    resource function get deleteFile(string fileName) returns string|error {
        // Noncompliant
        check file:remove(targetDirectory + fileName);

        io:println("File" + targetDirectory + fileName + "deleted successfully!");
        return "File" + targetDirectory + fileName + "deleted successfully!";
    }
}

// Define the service endpoint (Secure and Compliant)
// service / on endpoint {
//     resource function get deleteFile(string fileName) returns string|error {
//         // Compliant
//         // Retrieve the normalized absolute path of the user provided file
//         string absoluteUserFilePath = check file:getAbsolutePath(targetDirectory + fileName);
//         string normalizedAbsoluteUserFilePath = check file:normalizePath(absoluteUserFilePath, file:CLEAN);

//         // Check whether the user provided file exists
//         boolean fileExists = check file:test(normalizedAbsoluteUserFilePath, file:EXISTS);
//         if !fileExists {
//             return "File does not exist!";
//         }

//         // Retrieve the normalized absolute path of parent directory of the user provided file
//         string canonicalDestinationPath = check file:parentPath(normalizedAbsoluteUserFilePath);
//         string normalizedCanonicalDestinationPath = check file:normalizePath(canonicalDestinationPath, file:CLEAN);

//         // Retrieve the normalized absolute path of the target directory
//         string absoluteTargetFilePath = check file:getAbsolutePath(targetDirectory);
//         string normalizedTargetDirectoryPath = check file:normalizePath(absoluteTargetFilePath, file:CLEAN);

//         // Perform comparison of user provided file path and target directory path
//         boolean dirMatch = normalizedTargetDirectoryPath.equalsIgnoreCaseAscii(normalizedCanonicalDestinationPath);
//         if !dirMatch {
//             return "Entry is not in the target directory!";
//         }

//         check file:remove(normalizedAbsoluteUserFilePath);

//         // ...

//         io:println("File" + targetDirectory + fileName + "deleted successfully!");
//         return "File" + targetDirectory + fileName + "deleted successfully!";
//     }
// }
