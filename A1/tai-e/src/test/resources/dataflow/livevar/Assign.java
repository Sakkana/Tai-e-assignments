class Assign {

    int assign(int a, int b, int c) {
        int d = a + b;  // 活跃变量： []
        b = d;          // 活跃变量： [a d]
        c = a;          // 活跃变量： [a b]
        return b;       // 活跃变量： [b]
    }
}
