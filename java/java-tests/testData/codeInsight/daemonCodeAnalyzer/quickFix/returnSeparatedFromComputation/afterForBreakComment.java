// "Move 'return' to computation of the value of 'n'" "true"
class T {
    int f(int a[]) {
        int n = 0;
        for (int i=0; i<a.length; i++) {
            if (n < a[i]) n = a[i];
            if (n > 100) {
                return n; // at the end 1
            }
            if (n < 0) {
                return 0; // at the end 2
                /* inline */
            }
        }
        return n;
    }
}