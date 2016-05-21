package edu.princeton.safe.internal;

import org.junit.Assert;
import org.junit.Test;

public class UtilTest {
    @Test
    public void testHsl2RgbRed() {
        int[] expected = { 255, 0, 0 };
        int[] result = Util.hslToRgb(0, 1, 0.5);
        Assert.assertArrayEquals(expected, result);
    }

    @Test
    public void testHsl2RgbGreen() {
        int[] expected = { 0, 255, 0 };
        int[] result = Util.hslToRgb(1.0 / 3, 1, 0.5);
        Assert.assertArrayEquals(expected, result);
    }

    @Test
    public void testHsl2RgbBlue() {
        int[] expected = { 0, 0, 255 };
        int[] result = Util.hslToRgb(2.0 / 3, 1, 0.5);
        Assert.assertArrayEquals(expected, result);
    }

    @Test
    public void testHsl2RgbBlack() {
        int[] expected = { 0, 0, 0 };
        Assert.assertArrayEquals(expected, Util.hslToRgb(0, 0, 0));
        Assert.assertArrayEquals(expected, Util.hslToRgb(0, 1, 0));
        Assert.assertArrayEquals(expected, Util.hslToRgb(1, 0, 0));
        Assert.assertArrayEquals(expected, Util.hslToRgb(1, 1, 0));
    }

    @Test
    public void testHsl2RgbWhite() {
        int[] expected = { 255, 255, 255 };
        Assert.assertArrayEquals(expected, Util.hslToRgb(0, 0, 1));
        Assert.assertArrayEquals(expected, Util.hslToRgb(0, 1, 1));
        Assert.assertArrayEquals(expected, Util.hslToRgb(1, 0, 1));
        Assert.assertArrayEquals(expected, Util.hslToRgb(1, 1, 1));
    }
}
