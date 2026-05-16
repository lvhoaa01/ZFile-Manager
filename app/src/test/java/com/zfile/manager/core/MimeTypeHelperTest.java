package com.zfile.manager.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.zfile.manager.model.FileType;

import org.junit.Test;

public class MimeTypeHelperTest {

    @Test
    public void getExtension_normal() {
        assertEquals("txt", MimeTypeHelper.getExtension("foo.txt"));
        assertEquals("MP3", MimeTypeHelper.getExtension("song.MP3"));
        // lastIndexOf('.') is used, so multi-dot names return only the final segment
        assertEquals("gz", MimeTypeHelper.getExtension("archive.tar.gz"));
    }

    @Test
    public void getExtension_noDot() {
        assertNull(MimeTypeHelper.getExtension("README"));
        assertNull(MimeTypeHelper.getExtension(""));
    }

    @Test
    public void getExtension_trailingDot() {
        assertNull(MimeTypeHelper.getExtension("foo."));
    }

    @Test
    public void getFileType_image() {
        assertEquals(FileType.IMAGE, MimeTypeHelper.getFileTypeFromName("photo.jpg"));
        assertEquals(FileType.IMAGE, MimeTypeHelper.getFileTypeFromName("PHOTO.PNG"));
        assertEquals(FileType.IMAGE, MimeTypeHelper.getFileTypeFromName("icon.webp"));
        assertEquals(FileType.IMAGE, MimeTypeHelper.getFileTypeFromName("portrait.heic"));
    }

    @Test
    public void getFileType_video() {
        assertEquals(FileType.VIDEO, MimeTypeHelper.getFileTypeFromName("movie.mp4"));
        assertEquals(FileType.VIDEO, MimeTypeHelper.getFileTypeFromName("clip.mkv"));
    }

    @Test
    public void getFileType_audio() {
        assertEquals(FileType.AUDIO, MimeTypeHelper.getFileTypeFromName("song.mp3"));
        assertEquals(FileType.AUDIO, MimeTypeHelper.getFileTypeFromName("track.flac"));
    }

    @Test
    public void getFileType_document() {
        assertEquals(FileType.DOCUMENT, MimeTypeHelper.getFileTypeFromName("report.pdf"));
        assertEquals(FileType.DOCUMENT, MimeTypeHelper.getFileTypeFromName("essay.docx"));
    }

    @Test
    public void getFileType_archive() {
        assertEquals(FileType.ARCHIVE, MimeTypeHelper.getFileTypeFromName("source.zip"));
        assertEquals(FileType.ARCHIVE, MimeTypeHelper.getFileTypeFromName("bundle.7z"));
    }

    @Test
    public void getFileType_apk() {
        assertEquals(FileType.APK, MimeTypeHelper.getFileTypeFromName("install.apk"));
        assertEquals(FileType.APK, MimeTypeHelper.getFileTypeFromName("bundle.xapk"));
    }

    @Test
    public void getFileType_text() {
        assertEquals(FileType.TEXT, MimeTypeHelper.getFileTypeFromName("notes.txt"));
        assertEquals(FileType.TEXT, MimeTypeHelper.getFileTypeFromName("config.json"));
    }

    @Test
    public void getFileType_unknown() {
        assertEquals(FileType.UNKNOWN, MimeTypeHelper.getFileTypeFromName("data.xyz"));
        assertEquals(FileType.UNKNOWN, MimeTypeHelper.getFileTypeFromName("noextension"));
    }

    @Test
    public void getIconResourceName_allTypes() {
        assertEquals("ic_folder", MimeTypeHelper.getIconResourceName(FileType.FOLDER));
        assertEquals("ic_file_image", MimeTypeHelper.getIconResourceName(FileType.IMAGE));
        assertEquals("ic_file_video", MimeTypeHelper.getIconResourceName(FileType.VIDEO));
        assertEquals("ic_file_audio", MimeTypeHelper.getIconResourceName(FileType.AUDIO));
        assertEquals("ic_file_document", MimeTypeHelper.getIconResourceName(FileType.DOCUMENT));
        assertEquals("ic_file_archive", MimeTypeHelper.getIconResourceName(FileType.ARCHIVE));
        assertEquals("ic_file_apk", MimeTypeHelper.getIconResourceName(FileType.APK));
        assertEquals("ic_file_text", MimeTypeHelper.getIconResourceName(FileType.TEXT));
        assertEquals("ic_file_unknown", MimeTypeHelper.getIconResourceName(FileType.UNKNOWN));
    }
}
