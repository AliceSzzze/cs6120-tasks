#include <stdio.h>

// Function to calculate the factorial of a number recursively
unsigned long long factorial(int n) {
    if (n == 0 || n == 1) {
        return 1;
    } else {
        return n * factorial(n - 1);
    }
}

int main() {
    int n = 20;

    unsigned long long result = factorial(n);
    printf("Factorial of %d is %llu\n", n, result);

    return 0;
}
