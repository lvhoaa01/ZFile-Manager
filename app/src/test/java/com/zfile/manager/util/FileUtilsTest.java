package com.zfile.manager.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FileUtilsTest {

    @Test
    public void formatSize_bytes() {
        assertEquals("0 B", FileUtils.formatSize(0));
        assertEquals("1 B", FileUtils.formatSize(1));
        assertEquals("1023 B", FileUtils.formatSize(1023));
    }

    @Test
    public void formatSize_kilobytes() {
        assertEquals("1.0 KB", FileUtils.formatSize(1024));
        assertEquals("1.5 KB", FileUtils.formatSize(1536));
        assertEquals("1023.9 KB", FileUtils.formatSize(1024 * 1024 - 100));
    }

    @Test
    public void formatSize_megabytes() {
        assertEquals("1.0 MB", FileUtils.formatSize(1024L * 1024));
        assertEquals("1.5 MB", FileUtils.formatSize(1024L * 1024 + 512 * 1024));
    }

    @Test
    public void formatSize_gigabytes() {
        assertEquals("1.00 GB", FileUtils.formatSize(1024L * 1024 * 1024));
    }

    @Test
    public void formatSize_terabytes() {
        assertEquals("1.00 TB", FileUtils.formatSize(1024L * 1024 * 1024 * 1024));
    }

    @Test
    public void formatSize_negative_returnsDash() {
        assertEquals("—", FileUtils.formatSize(-1));
        assertEquals("—", FileUtils.formatSize(Long.MIN_VALUE));
    }

    @Test
    public void formatDate_zero_returnsDash() {
        assertEquals("—", FileUtils.formatDate(0));
        assertEquals("—", FileUtils.formatDate(-100));
    }

    @Test
    public void formatDate_positive_returnsNonEmpty() {
        String result = FileUtils.formatDate(System.currentTimeMillis());
        assertTrue("Date format should not be empty", result.length() > 0);
        assertTrue("Date format should not be dash", !"—".equals(result));
    }

    @Test
    public void formatPermissions_combinations() {
        assertEquals("rw-", FileUtils.formatPermissions(true, true));
        assertEquals("r--", FileUtils.formatPermissions(true, false));
        assertEquals("-w-", FileUtils.formatPermissions(false, true));
        assertEquals("---", FileUtils.formatPermissions(false, false));
    }
}
