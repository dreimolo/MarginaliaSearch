package nu.marginalia.storage;

import com.google.common.collect.Lists;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import nu.marginalia.storage.model.FileStorageBaseType;
import nu.marginalia.storage.model.FileStorageType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

@Testcontainers
@Execution(ExecutionMode.SAME_THREAD)
@Tag("slow")
public class FileStorageServiceTest {
    @Container
    static MariaDBContainer<?> mariaDBContainer = new MariaDBContainer<>("mariadb")
            .withDatabaseName("WMSA_prod")
            .withUsername("wmsa")
            .withPassword("wmsa")
            .withInitScript("db/migration/V23_07_0_004__file_storage.sql")
            .withNetworkAliases("mariadb");

    static HikariDataSource dataSource;
    static FileStorageService fileStorageService;

    static List<Path> tempDirs = new ArrayList<>();

    @BeforeAll
    public static void setup() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(mariaDBContainer.getJdbcUrl());
        config.setUsername("wmsa");
        config.setPassword("wmsa");

        dataSource = new HikariDataSource(config);

        // apply migrations

        List<String> migrations = List.of(
                "db/migration/V23_11_0_000__file_storage_node.sql",
                "db/migration/V23_11_0_002__file_storage_state.sql",
                "db/migration/V23_11_0_004__file_storage_base_type.sql"
                );
        for (String migration : migrations) {
            try (var resource = Objects.requireNonNull(ClassLoader.getSystemResourceAsStream(migration),
                    "Could not load migration script " + migration);
                 var conn = dataSource.getConnection();
                 var stmt = conn.createStatement()
            ) {
                String script = new String(resource.readAllBytes());
                String[] cmds = script.split("\\s*;\\s*");
                for (String cmd : cmds) {
                    if (cmd.isBlank())
                        continue;
                    System.out.println(cmd);
                    stmt.executeUpdate(cmd);
                }
            }
            catch (IOException|SQLException ex) {

            }
        }
    }


    @BeforeEach
    public void setupEach() {
        // clean up any file storage overrides
        for (FileStorageType type : FileStorageType.values()) {
            System.setProperty(type.overrideName(), "");
        }

        fileStorageService = new FileStorageService(dataSource, 0);
    }

    @AfterEach
    public void tearDownEach() {
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM FILE_STORAGE");
            stmt.execute("DELETE FROM FILE_STORAGE_BASE");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterAll
    public static void teardown() {
        dataSource.close();

        Lists.reverse(tempDirs).forEach(path -> {
            try {
                System.out.println("Deleting " + path);
                Files.delete(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private Path createTempDir() {
        try {
            Path dir = Files.createTempDirectory("file-storage-test");
            tempDirs.add(dir);
            return dir;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    public void testOverride() throws SQLException {
        System.setProperty(FileStorageType.BACKUP.overrideName(), "/tmp");
        System.out.println(FileStorageType.BACKUP.overrideName());
        fileStorageService = new FileStorageService(dataSource, 0);
        Assertions.assertEquals(Path.of("/tmp"), fileStorageService.getStorageByType(FileStorageType.BACKUP).asPath());
    }
    @Test
    public void testCreateBase() throws SQLException {
        String name = "test-" + UUID.randomUUID();

        var storage = new FileStorageService(dataSource, 0);
        var base = storage.createStorageBase(name, createTempDir(), FileStorageBaseType.WORK);

        Assertions.assertEquals(name, base.name());
        Assertions.assertEquals(FileStorageBaseType.WORK, base.type());
    }

    @Test
    public void testAllocateTemp() throws IOException, SQLException {
        String name = "test-" + UUID.randomUUID();

        var storage = new FileStorageService(dataSource, 0);

        var base = storage.createStorageBase(name, createTempDir(), FileStorageBaseType.STORAGE);
        var fileStorage = storage.allocateTemporaryStorage(base, FileStorageType.CRAWL_DATA, "xyz", "thisShouldSucceed");
        System.out.println("Allocated " + fileStorage.asPath());
        Assertions.assertTrue(Files.exists(fileStorage.asPath()));
        tempDirs.add(fileStorage.asPath());
    }


}