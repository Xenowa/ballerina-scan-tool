import ballerina/http;
import ballerina/io;
import ballerina/sql;
import ballerinax/mysql;
import ballerinax/mysql.driver as _; // Import the MySQL driver

// Retrieve config variables
configurable string USER = ?;
configurable string PASSWORD = ?;
configurable string HOST = ?;
configurable int PORT = ?;
configurable string DATABASE = ?;

// Define format of the records in the users table
type User record {
    string username;
    string password;
    string address;
    int age;
};

// Define the service endpoint
service / on new http:Listener(8080) {
    resource function get userDetails() returns string|error? {
        // Connect to the MySQL database
        mysql:Client dbClient = check new (host = HOST,
            user = USER,
            password = PASSWORD,
            port = PORT,
            database = DATABASE
        );

        // Noncompliant
        // Create the query
        sql:ParameterizedQuery query = `SELECT * FROM users WHERE username = 'jhon' AND password = 'jhon@123'`;

        // ...

        // Run the query and retrieve the results
        User|sql:Error result = dbClient->queryRow(query);

        // close the database connection after operation
        check dbClient.close();

        // if everything went well return the string user address
        if (result is User) {
            io:println(result.toJson());
            return result.toJsonString();
        }

        io:println("Error: ", result);
        return "Operation unsuccessful";
    }
}

// Define the service endpoint (Secure and Compliant)
// service / on new http:Listener(8080) {
//     resource function get userDetails(string username, string password) returns string|error? {
//         // Connect to the MySQL database
//         mysql:Client dbClient = check new (host = HOST,
//             user = USER,
//             password = PASSWORD,
//             port = PORT,
//             database = DATABASE
//         );

//         // Compliant
//         // Create the query
//         sql:ParameterizedQuery query = `SELECT *
//             FROM users
//             WHERE username = ${username}
//             AND password = ${password}`;

//         // ...

//         // Run the query and retrieve the results
//         User|sql:Error result = dbClient->queryRow(query);

//         // close the database connection after operation
//         check dbClient.close();

//         // if everything went well return the string user address
//         if (result is User) {
//             io:println(result.toJson());
//             return result.toJsonString();
//         }

//         io:println("Error: ", result);
//         return "Operation unsuccessful";
//     }
// }
