import ballerina/crypto;
import ballerina/io;
import ballerina/os;

public function checkRule() {
    // Noncompliant
    string inputString = "s3cr37";
    byte[] key = inputString.toBytes();

    string dataString = "Hello Ballerina!";
    byte[] hash = dataString.toBytes();
    byte[] hashedString = crypto:hashSha256(key, hash);

    io:println("Hashed string: " + hashedString.toString());
}

// Compliant
configurable string inputString = ?;
configurable string dataString = ?;

public function compliantSolution1() {
    byte[] key = inputString.toBytes();
    byte[] hash = dataString.toBytes();
    byte[] hashedString = crypto:hashSha256(key, hash);

    io:println("Hashed string: " + hashedString.toString());
}

public function compliantSolution2() {
    // Compliant
    byte[] key = os:getEnv("INPUT_STRING").toBytes();
    byte[] hash = os:getEnv("DATA_STRING").toBytes();
    byte[] hashedString = crypto:hashSha256(key, hash);

    io:println("Hashed string: " + hashedString.toString());
}
