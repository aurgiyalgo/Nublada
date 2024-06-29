package me.lofienjoyer.valkyrie;

public class ValkyrieMath {

    public static float intbound(float a, float b) {
        if (b < 0) {
            return intbound(-a, -b);
        } else {
            a = mod(a, 1);
            return (1-a)/b;
        }
    }

    public static float mod(float value, float modulus) {
        return (value % modulus + modulus) % modulus;
    }

    public static int signum(float x) {
        return x > 0 ? 1 : x < 0 ? -1 : 0;
    }

}
