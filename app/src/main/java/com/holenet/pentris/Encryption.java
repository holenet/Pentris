package com.holenet.pentris;

import java.util.Random;

public class Encryption {
    private final static int INTERVAL = 10;
    private final static int BITS = 31;

    public static String encode(int c) {
        Random random = new Random();
        String s = "";
        for(int i=INTERVAL*BITS-1; i>=0; i--) {
            if(i%INTERVAL<INTERVAL-1)
                s = (char)(48+17*random.nextInt(5)+random.nextInt(8)+2) + s;
            else {
                s = (char)(48+17*random.nextInt(5)+c%2) + s;
                c /= 2;
            }
        }
        return s;
    }

    public static int decode(String s) {
        int l = s.length();
        if(l!=INTERVAL*BITS)
            return -1;
        int c = 0;
        for(int i=0; i<l; i++) {
            if(i%INTERVAL<INTERVAL-1)
                continue;
            int k = (s.charAt(i)-48)%17;
            if(k>1)
                return -i-1000;
            c *= 2;
            c += k;
        }
        return c;
    }
}