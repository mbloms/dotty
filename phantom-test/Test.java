class Test {
    public static void main(String [] args){
        Test a = new Test();
        Test b = new Test(a);
        b.y = 5;
        System.out.println(a.x);
        System.out.println(b.x);
    }
    public final int x;
    public int y;

    public Test() {
        x = -1;
        new Test(this);
    }
    public Test(Test t){
        y = 1;
        t.y = 2+y;
        x = 3;
    }
}