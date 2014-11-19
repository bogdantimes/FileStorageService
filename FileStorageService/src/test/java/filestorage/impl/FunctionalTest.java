package filestorage.impl;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

import static filestorage.impl.BasicFileStorageService.DATA_FOLDER_NAME;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FunctionalTest {

    private static final String STORAGE_ROOT = "storage";
    private static final int MAX_DISK_SPACE = 10240;
    private static Random random = new Random();

    private static String getRandomFileName() {
        int nameLength = random.nextInt(5) + 10;
        StringBuilder fileName = new StringBuilder();
        while (nameLength-- > 0) {
            final char randomChar = (char) (random.nextInt('z' - 'a') + 'a');
            fileName.append(randomChar);
        }
        return fileName.toString();
    }

    private static ByteArrayInputStream getRandomData() {
        return new ByteArrayInputStream(new byte[random.nextInt(400) + 100]);
    }

    @Test
    public void testStartService() throws Exception {
        BasicFileStorageService fileStorageService = new BasicFileStorageService(MAX_DISK_SPACE, STORAGE_ROOT);

        fileStorageService.startService();

        assertTrue(fileStorageService.serviceIsStarted());
    }

    @Test
    public void testStopService() throws Exception {
        BasicFileStorageService fileStorageService = new BasicFileStorageService(MAX_DISK_SPACE, STORAGE_ROOT);

        fileStorageService.startService();
        fileStorageService.stopService();

        assertTrue(!fileStorageService.serviceIsStarted());
    }

    @Test
    public void testSaveFile() throws Exception {
        final BasicFileStorageService fileStorageService = new BasicFileStorageService(MAX_DISK_SPACE, STORAGE_ROOT);
        fileStorageService.startService();

        final String filename = getRandomFileName();

        fileStorageService.saveFile(filename, getRandomData());

        try (final InputStream inputStream = fileStorageService.readFile(filename)) {
            assertTrue(String.valueOf(inputStream), inputStream != null);
        } catch (FileNotFoundException e) {
            assertTrue(e.getMessage(), false);
        }
    }

    @Test
    public void testExpiredFileDeleted() throws Exception {
        final BasicFileStorageService fileStorageService = new BasicFileStorageService(MAX_DISK_SPACE, STORAGE_ROOT);
        fileStorageService.startService();

        final String filename = getRandomFileName();
        final long lifeTime = 1000;

        fileStorageService.saveFile(filename, getRandomData(), lifeTime);

        Thread.sleep(lifeTime + LifeTimeWatcher.SLEEP_TIME * 2);

        try (final InputStream inputStream = fileStorageService.readFile(filename)) {
            assertFalse("File still exist", inputStream != null);
        } catch (FileNotFoundException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testReadFile() throws Exception {
        final BasicFileStorageService fileStorageService = new BasicFileStorageService(MAX_DISK_SPACE, STORAGE_ROOT);
        fileStorageService.startService();

        final String filename = getRandomFileName();
        final byte testByte = 4;
        fileStorageService.saveFile(filename, new ByteArrayInputStream(new byte[]{testByte}));

        try (final InputStream inputStream = fileStorageService.readFile(filename)) {
            final int actual = inputStream.read();
            assertTrue("value expected: " + testByte + ", value read: " + actual, actual == testByte);
        } catch (FileNotFoundException e) {
            assertTrue(false);
        }
    }

    @Test
    public void testDeleteFile() throws Exception {
        final BasicFileStorageService fileStorageService = new BasicFileStorageService(MAX_DISK_SPACE, STORAGE_ROOT);
        fileStorageService.startService();

        final String filename = getRandomFileName();

        fileStorageService.saveFile(filename, new ByteArrayInputStream(new byte[random.nextInt(500)]));
        fileStorageService.deleteFile(filename);

        try (final InputStream inputStream = fileStorageService.readFile(filename)) {
            assertFalse("File still exist", inputStream != null);
        } catch (FileNotFoundException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testGetFreeStorageSpace() throws Exception {
        final BasicFileStorageService fileStorageService = new BasicFileStorageService(MAX_DISK_SPACE, STORAGE_ROOT);
        fileStorageService.startService();

        final long freeSpaceBefore = fileStorageService.getFreeStorageSpace();

        final String fileName1 = getRandomFileName();
        fileStorageService.saveFile(fileName1, getRandomData());
        final String fileName2 = getRandomFileName();
        fileStorageService.saveFile(fileName2, getRandomData());
        final String fileName3 = getRandomFileName();
        fileStorageService.saveFile(fileName3, getRandomData());

        long total = 0;
        long deleted;

        try (InputStream inputStream1 = fileStorageService.readFile(fileName1);
             InputStream inputStream2 = fileStorageService.readFile(fileName2);
             InputStream inputStream3 = fileStorageService.readFile(fileName3)) {
            total += inputStream1.available();
            total += deleted = inputStream2.available();
            total += inputStream3.available();
        }

        fileStorageService.deleteFile(fileName2);

        total -= deleted;

        final long expected = freeSpaceBefore - total;
        final long actual = fileStorageService.getFreeStorageSpace();

        assertTrue("Free space expected: " + expected + ", actual: " + actual, actual == expected);
    }

    @Test
    public void testPurge() throws Exception {
        final BasicFileStorageService fileStorageService = new BasicFileStorageService(MAX_DISK_SPACE, STORAGE_ROOT);
        fileStorageService.startService();

        while (fileStorageService.getFreeStorageSpace() > MAX_DISK_SPACE * 0.1) {
            final String fileName = getRandomFileName();
            fileStorageService.saveFile(fileName, getRandomData());
        }

        final float percents = 0.5f;
        fileStorageService.purge(percents);

        final long actual = fileStorageService.getFreeStorageSpace();
        final long expected = (long) (MAX_DISK_SPACE * percents - fileStorageService.getSystemFolderSize());

        assertTrue("Free space expected: " + expected + ", actual: " + actual, actual >= expected);
    }

    @Test
    public void testGetSystemFolderSize() throws Exception {
        final BasicFileStorageService fileStorageService = new BasicFileStorageService(MAX_DISK_SPACE, STORAGE_ROOT);
        fileStorageService.startService();

        fileStorageService.saveFile(getRandomFileName(), getRandomData(), 40000);
        fileStorageService.saveFile(getRandomFileName(), getRandomData(), 40000);
        fileStorageService.saveFile(getRandomFileName(), getRandomData(), 40000);

        final Path systemFolderPath = Paths.get(STORAGE_ROOT, BasicFileStorageService.SYSTEM_FOLDER_NAME);
        final File systemFolder = new File(String.valueOf(systemFolderPath));
        final File[] files = systemFolder.listFiles();

        long expected = 0;
        if (files != null)
            for (File file : files) {
                expected += file.length();
            }

        final long actual = fileStorageService.getSystemFolderSize();

        assertTrue("Expected: " + expected + ", actual: " + actual, actual == expected);
    }

    @Test
    public void testClearEmptyDirectories() throws Exception {
        final BasicFileStorageService fileStorageService = new BasicFileStorageService(MAX_DISK_SPACE, STORAGE_ROOT);
        fileStorageService.startService();

        final String fileName = getRandomFileName();
        fileStorageService.saveFile(fileName, getRandomData());
        fileStorageService.deleteFile(fileName);

        fileStorageService.deleteEmptyDirectories();

        final File dataDirectory = new File(String.valueOf(Paths.get(STORAGE_ROOT, DATA_FOLDER_NAME)));
        Deque<File> stack = new ArrayDeque<>();
        stack.push(dataDirectory);


        while (!stack.isEmpty()) {
            final File[] files = stack.pop().listFiles();
            if (files == null) continue;
            for (File file : files) {
                if (file.isDirectory()) {
                    if (file.list().length == 0)
                        assertTrue("Directory '" + file.getPath() + "' empty but exist", false);
                    else
                        stack.push(file);
                }
            }
        }
    }
}