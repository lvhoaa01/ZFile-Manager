package com.zfile.manager.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * JVM-level integration test for {@link FileTransferService} on a temp dir.
 * Skips behaviours that require Android-specific APIs (e.g. SAF).
 */
public class FileTransferServiceTest {

    private File tmpDir;
    private FileTransferService transfer;

    @Before
    public void setUp() throws IOException {
        tmpDir = Files.createTempDirectory("zfile-transfer-test").toFile();
        transfer = new FileTransferService();
    }

    @After
    public void tearDown() {
        deleteRecursive(tmpDir);
    }

    @Test
    public void isValidFilename_rejectsBadInput() {
        assertFalse(FileTransferService.isValidFilename(null));
        assertFalse(FileTransferService.isValidFilename(""));
        assertFalse(FileTransferService.isValidFilename("   "));
        assertFalse(FileTransferService.isValidFilename("."));
        assertFalse(FileTransferService.isValidFilename(".."));
        assertFalse(FileTransferService.isValidFilename("foo/bar"));
        assertFalse(FileTransferService.isValidFilename("foo\\bar"));
        assertFalse(FileTransferService.isValidFilename("foo\0bar"));

        StringBuilder over = new StringBuilder();
        for (int i = 0; i < 256; i++) over.append('a');
        assertFalse(FileTransferService.isValidFilename(over.toString()));
    }

    @Test
    public void isValidFilename_acceptsNormalNames() {
        assertTrue(FileTransferService.isValidFilename("report.pdf"));
        assertTrue(FileTransferService.isValidFilename(".hidden"));
        assertTrue(FileTransferService.isValidFilename("文件.txt"));
        assertTrue(FileTransferService.isValidFilename("with spaces.md"));
    }

    @Test
    public void createFolder_validName_creates() {
        assertTrue(transfer.createFolder(tmpDir.getAbsolutePath(), "newdir"));
        File created = new File(tmpDir, "newdir");
        assertTrue(created.isDirectory());
    }

    @Test
    public void createFolder_invalidName_rejected() {
        assertFalse(transfer.createFolder(tmpDir.getAbsolutePath(), ""));
        assertFalse(transfer.createFolder(tmpDir.getAbsolutePath(), "foo/bar"));
        assertFalse(new File(tmpDir, "foo").exists());
    }

    @Test
    public void createFolder_alreadyExists_returnsFalse() throws IOException {
        new File(tmpDir, "exists").mkdir();
        assertFalse(transfer.createFolder(tmpDir.getAbsolutePath(), "exists"));
    }

    @Test
    public void createFile_validName_creates() throws IOException {
        assertTrue(transfer.createFile(tmpDir.getAbsolutePath(), "note.txt"));
        File created = new File(tmpDir, "note.txt");
        assertTrue(created.isFile());
        assertEquals(0L, created.length());
    }

    @Test
    public void rename_invalidName_rejected() throws IOException {
        File f = new File(tmpDir, "orig.txt");
        f.createNewFile();
        assertFalse(transfer.rename(f.getAbsolutePath(), ""));
        assertFalse(transfer.rename(f.getAbsolutePath(), "with/slash"));
        assertTrue(f.exists());
    }

    @Test
    public void rename_valid_works() throws IOException {
        File f = new File(tmpDir, "orig.txt");
        f.createNewFile();
        assertTrue(transfer.rename(f.getAbsolutePath(), "renamed.txt"));
        assertFalse(f.exists());
        assertTrue(new File(tmpDir, "renamed.txt").exists());
    }

    @Test
    public void copyFiles_singleFile_copiesContent() throws IOException {
        File src = new File(tmpDir, "src.txt");
        Files.write(src.toPath(), "hello world".getBytes());
        File destDir = new File(tmpDir, "destdir");
        destDir.mkdir();

        transfer.copyFiles(
                Collections.singletonList(src.getAbsolutePath()),
                destDir.getAbsolutePath(),
                null,
                new AtomicBoolean(false));

        File copy = new File(destDir, "src.txt");
        assertTrue(copy.exists());
        assertEquals(11L, copy.length());
        assertTrue("Original should remain", src.exists());
    }

    @Test
    public void copyFiles_directory_recursive() throws IOException {
        File srcDir = new File(tmpDir, "srcdir");
        srcDir.mkdir();
        new File(srcDir, "a.txt").createNewFile();
        File subdir = new File(srcDir, "sub");
        subdir.mkdir();
        new File(subdir, "b.txt").createNewFile();

        File destDir = new File(tmpDir, "destdir");
        destDir.mkdir();

        transfer.copyFiles(
                Collections.singletonList(srcDir.getAbsolutePath()),
                destDir.getAbsolutePath(),
                null,
                new AtomicBoolean(false));

        assertTrue(new File(destDir, "srcdir/a.txt").exists());
        assertTrue(new File(destDir, "srcdir/sub/b.txt").exists());
    }

    @Test
    public void moveFiles_sameVolume_fastPathRenames() throws IOException {
        File src = new File(tmpDir, "moveme.txt");
        Files.write(src.toPath(), "x".getBytes());
        File destDir = new File(tmpDir, "destdir");
        destDir.mkdir();

        transfer.moveFiles(
                Collections.singletonList(src.getAbsolutePath()),
                destDir.getAbsolutePath(),
                null,
                new AtomicBoolean(false));

        assertFalse("Source should be gone", src.exists());
        assertTrue("Dest should exist", new File(destDir, "moveme.txt").exists());
    }

    @Test
    public void deleteRecursive_directory_removesEverything() throws IOException {
        File dir = new File(tmpDir, "kill");
        dir.mkdir();
        new File(dir, "a.txt").createNewFile();
        new File(dir, "b.txt").createNewFile();
        File sub = new File(dir, "sub");
        sub.mkdir();
        new File(sub, "c.txt").createNewFile();

        assertTrue(transfer.deleteRecursive(dir.getAbsolutePath()));
        assertFalse(dir.exists());
    }

    @Test
    public void copyFiles_uniqueDestinationOnCollision() throws IOException {
        File src = new File(tmpDir, "dup.txt");
        Files.write(src.toPath(), "first".getBytes());
        File destDir = new File(tmpDir, "destdir");
        destDir.mkdir();
        File preExisting = new File(destDir, "dup.txt");
        Files.write(preExisting.toPath(), "exists".getBytes());

        transfer.copyFiles(
                Collections.singletonList(src.getAbsolutePath()),
                destDir.getAbsolutePath(),
                null,
                new AtomicBoolean(false));

        assertTrue(preExisting.exists());
        File renamed = new File(destDir, "dup (2).txt");
        assertTrue("Should create dup (2).txt instead of overwriting", renamed.exists());
    }

    @Test
    public void copyFiles_multipleSources() throws IOException {
        File a = new File(tmpDir, "a.txt"); a.createNewFile();
        File b = new File(tmpDir, "b.txt"); b.createNewFile();
        File destDir = new File(tmpDir, "destdir");
        destDir.mkdir();

        transfer.copyFiles(
                Arrays.asList(a.getAbsolutePath(), b.getAbsolutePath()),
                destDir.getAbsolutePath(),
                null,
                new AtomicBoolean(false));

        assertTrue(new File(destDir, "a.txt").exists());
        assertTrue(new File(destDir, "b.txt").exists());
    }

    private static void deleteRecursive(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) for (File c : children) deleteRecursive(c);
        }
        f.delete();
    }
}
