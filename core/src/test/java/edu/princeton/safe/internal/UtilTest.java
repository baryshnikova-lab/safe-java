package edu.princeton.safe.internal;

import org.junit.Assert;
import org.junit.Test;

public class UtilTest {
    static final double DEFAULT_DELTA = 0.0001;

    @Test
    public void testHsl2RgbRed() {
        double[] expected = { 1, 0, 0 };
        double[] result = Util.hslToRgb(0, 1, 0.5);
        Assert.assertArrayEquals(expected, result, DEFAULT_DELTA);
    }

    @Test
    public void testHsl2RgbGreen() {
        double[] expected = { 0, 1, 0 };
        double[] result = Util.hslToRgb(1.0 / 3, 1, 0.5);
        Assert.assertArrayEquals(expected, result, DEFAULT_DELTA);
    }

    @Test
    public void testHsl2RgbBlue() {
        double[] expected = { 0, 0, 1 };
        double[] result = Util.hslToRgb(2.0 / 3, 1, 0.5);
        Assert.assertArrayEquals(expected, result, DEFAULT_DELTA);
    }

    @Test
    public void testHsl2RgbBlack() {
        double[] expected = { 0, 0, 0 };
        Assert.assertArrayEquals(expected, Util.hslToRgb(0, 0, 0), DEFAULT_DELTA);
        Assert.assertArrayEquals(expected, Util.hslToRgb(0, 1, 0), DEFAULT_DELTA);
        Assert.assertArrayEquals(expected, Util.hslToRgb(1, 0, 0), DEFAULT_DELTA);
        Assert.assertArrayEquals(expected, Util.hslToRgb(1, 1, 0), DEFAULT_DELTA);
    }

    @Test
    public void testHsl2RgbWhite() {
        double[] expected = { 1, 1, 1 };
        Assert.assertArrayEquals(expected, Util.hslToRgb(0, 0, 1), DEFAULT_DELTA);
        Assert.assertArrayEquals(expected, Util.hslToRgb(0, 1, 1), DEFAULT_DELTA);
        Assert.assertArrayEquals(expected, Util.hslToRgb(1, 0, 1), DEFAULT_DELTA);
        Assert.assertArrayEquals(expected, Util.hslToRgb(1, 1, 1), DEFAULT_DELTA);
    }
}
