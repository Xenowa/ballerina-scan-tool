// Non-compliant as the function has too many parameters
public function tooManyParametersFunc(int a, int b, int c, int d, int e, int f, int g, int h) returns int {
    return a + b + c + d + e + f + g;
}

public function main() {
    int x = tooManyParametersFunc(1, 2, 3, 4, 5, 6, 7, 8);
}
