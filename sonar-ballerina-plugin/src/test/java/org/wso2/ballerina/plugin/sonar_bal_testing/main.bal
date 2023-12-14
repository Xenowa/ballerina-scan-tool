// ================================
// Ballerina service related checks
// ================================
// Insecure endpoint rule testing
// import sonar_bal_testing.insecureEndpoint as _;
// Database injection rule testing
// import sonar_bal_testing.databaseInjection as _;
// Filepath injection rule testing
import sonar_bal_testing.functionPathInjection as _;

// import sonar_bal_testing.emptyFunction;
// import sonar_bal_testing.panicChecker;
// import sonar_bal_testing.tooManyParameters;
// import sonar_bal_testing.insecureEndpoint;
// import sonar_bal_testing.credentialsNotHardCoded;

public function main() {
    // ==============================
    // Other Ballerina related checks
    // ==============================
    // int result = tooManyParameters:checkRule(1, 2, 3, 4, 5, 6, 7, 8);
    // panicChecker:checkRule();
    // emptyFunction:checkRule();
    // credentialsNotHardCoded:checkRule();
}
