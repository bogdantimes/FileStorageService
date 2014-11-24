package filestorage;

import filestorage.impl.exception.InvalidPercentsValueException;
import filestorage.impl.exception.NotEnoughFreeSpaceException;
import filestorage.impl.exception.StorageCorruptedException;
import filestorage.impl.exception.StorageServiceIsNotStartedError;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;

/**
 * @author Bogdan Kovalev
 */
public interface FileStorageService {

    void saveFile(String key, InputStream inputStream)
            throws StorageServiceIsNotStartedError, NotEnoughFreeSpaceException, StorageCorruptedException, FileAlreadyExistsException;

    void saveFile(String key, InputStream inputStream, long liveTimeMillis)
            throws FileAlreadyExistsException, StorageServiceIsNotStartedError, NotEnoughFreeSpaceException, StorageCorruptedException;

    InputStream readFile(String key) throws StorageServiceIsNotStartedError, FileNotFoundException;

    void deleteFile(String key) throws StorageServiceIsNotStartedError;

    long getFreeStorageSpaceInBytes() throws StorageServiceIsNotStartedError;

    float getFreeStorageSpaceInPercents() throws StorageServiceIsNotStartedError;

    void purge(float neededFreeSpaceInPercents) throws StorageServiceIsNotStartedError, InvalidPercentsValueException;

    void purge(long neededFreeSpaceInBytes) throws StorageServiceIsNotStartedError;
}
