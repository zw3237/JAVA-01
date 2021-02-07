package localvariables;

public class LocalVariableTest {
    public static void main(String[] args) {
        MovingAverage movingAverage = new MovingAverage();
        int num1 = 1;
        int num2 = 2;
        movingAverage.submit(num1);
        movingAverage.submit(num2);
        double avg = movingAverage.getAvg();
    }
}
