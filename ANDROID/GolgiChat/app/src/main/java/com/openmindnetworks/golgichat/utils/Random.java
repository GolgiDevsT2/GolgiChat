package com.openmindnetworks.golgichat.utils;

/**
 * Created by derekdoherty on 15/01/2015.
 */


public class Random {
    private static long x = 123456789;

    public static void seed(long seed){
        x = seed;
    }

    public static long next()
    {

        long llt;
        long mask = 0;

        //if(mask == 0){
            mask = 0x7fffffffffffffffL;
        //}

        x ^= (x << 21);
        llt = (x >> 1) & mask;
        x ^= (llt >> 34);
        x ^= (x << 4);

        return x;
    }

    public static int nextWithMax(int max)
    {
        int result = (int)(next() % max);
        return (result < 0) ? -result : result;
    }

    public static String genRandomStringWithLength(int length)
    {
        seed(System.currentTimeMillis());
        String str = new String("");

        for (int i = 0; i < length; i++)
        {
            str += (char)('A' + nextWithMax('z' - 'A'));
        }
        return str;
    }
}

