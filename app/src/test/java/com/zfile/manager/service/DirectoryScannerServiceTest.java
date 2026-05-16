package com.zfile.manager.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.zfile.manager.model.FileItem;
import com.zfile.manager.model.SortCriteria;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * JVM-level integration test for {@link DirectoryScannerService}: builds a
 * temp directory tree, then asserts scan output for various flag combinations.
 */
public class DirectoryScannerServiceTest {

    private File tmpDir;
    private DirectoryScannerService scanner;

    @Before
    public void setUp() throws IOException {
        tmpDir = Files.createTempDirectory("zfile-scanner-test").toFile();
        scanner = new DirectoryScannerService();
    }

    @After
    public void tearDown() {
        deleteRecursive(tmpDir);
    }

    @Test
    public void scan_emptyDirectory_returnsEmptyList() {
        List<FileItem> result = scanner.scanDirectory(tmpDir.getAbsolutePath(),
                false, SortCriteria.NAME_ASC, true);
        assertTrue(result.isEmpty());
    }

    @Test
    public void scan_nonExistent_returnsEmptyList() {
        List<FileItem> result = scanner.scanDirectory(
                tmpDir.getAbsolutePath() + "/does-not-exist",
                false, SortCriteria.NAME_ASC, true);
        assertTrue(result.isEmpty());
    }

    @Test
    public void scan_filtersHiddenWhenFlagOff() throws IOException {
        new File(tmpDir, "visible.txt").createNewFile();
        new File(tmpDir, ".hidden").createNewFile();

        List<FileItem> withoutHidden = scanner.scanDirectory(tmpDir.getAbsolutePath(),
                false, SortCriteria.NAME_ASC, true);
        assertEquals(1, withoutHidden.size());
        assertEquals("visible.txt", withoutHidden.get(0).getName());

        List<FileItem> withHidden = scanner.scanDirectory(tmpDir.getAbsolutePath(),
                true, SortCriteria.NAME_ASC, true);
        assertEquals(2, withHidden.size());
    }

    @Test
    public void scan_foldersFirstThenFiles() throws IOException {
        new File(tmpDir, "z-file.txt").createNewFile();
        new File(tmpDir, "a-folder").mkdir();

        List<FileItem> result = scanner.scanDirectory(tmpDir.getAbsolutePath(),
                false, SortCriteria.NAME_ASC, true);
        assertEquals(2, result.size());
        assertTrue("First should be folder", result.get(0).isDirectory());
        assertFalse("Second should be file", result.get(1).isDirectory());
    }

    @Test
    public void scan_sortByName_ascending() throws IOException {
        new File(tmpDir, "c.txt").createNewFile();
        new File(tmpDir, "a.txt").createNewFile();
        new File(tmpDir, "b.txt").createNewFile();

        List<FileItem> result = scanner.scanDirectory(tmpDir.getAbsolutePath(),
                false, SortCriteria.NAME_ASC, false);
        assertEquals("a.txt", result.get(0).getName());
        assertEquals("b.txt", result.get(1).getName());
        assertEquals("c.txt", result.get(2).getName());
    }

    @Test
    public void scan_sortBySize_descending() throws IOException {
        writeFile(new File(tmpDir, "small.txt"), 10);
        writeFile(new File(tmpDir, "big.txt"), 1000);
        writeFile(new File(tmpDir, "medium.txt"), 100);

        List<FileItem> result = scanner.scanDirectory(tmpDir.getAbsolutePath(),
                false, SortCriteria.SIZE_DESC, false);
        assertEquals("big.txt", result.get(0).getName());
        assertEquals("medium.txt", result.get(1).getName());
        assertEquals("small.txt", result.get(2).getName());
    }

    @Test
    public void exists_isDirectory_smoke() throws IOException {
        File file = new File(tmpDir, "f.txt");
        file.createNewFile();
        assertTrue(scanner.exists(file.getAbsolutePath()));
        assertFalse(scanner.isDirectory(file.getAbsolutePath()));
        assertTrue(scanner.isDirectory(tmpDir.getAbsolutePath()));
    }

    private static void writeFile(File f, int bytes) throws IOException {
        byte[] payload = new byte[bytes];
        Files.write(f.toPath(), payload);
    }

    private static void deleteRecursive(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) for (File c : children) deleteRecursive(c);
        }
        assertNotNull(f);
        f.delete();
    }
}
