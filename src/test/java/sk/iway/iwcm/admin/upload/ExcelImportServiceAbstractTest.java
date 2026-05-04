package sk.iway.iwcm.admin.upload;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.jspecify.annotations.NonNull;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import sk.iway.basecms.contact.ContactEntity;
import sk.iway.basecms.contact.ContactRepository;
import sk.iway.basecms.contact.excelimport.ExcelImportService;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.spring.SpringApplication;

import java.io.*;
import java.util.Optional;

// https://www.baeldung.com/java-beforeall-afterall-non-static
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {SpringApplication.class, UploadSpringConfig.class})
@WebAppConfiguration
public class ExcelImportServiceAbstractTest {
    @Autowired
    private ExcelImportService service;

    @Autowired
    private ContactRepository repository;

    @BeforeAll
    public void setup() {
        Logger.setLevel(Level.ERROR);
        Logger.setLevel("sk.iway.basecms", Level.DEBUG);
        Logger.setLevel("sk.iway.iwcm.admin.upload", Level.TRACE);

        Constants.setServletContext(new MockServletContext("Webjet"){
            @Override
            public String getRealPath(@NonNull String path)
            {
                String basePath = System.getProperty("webjetTestBasepath");
                if (Tools.isEmpty(basePath)) {
                    basePath = "./src/main/webapp";
                }
                return new File(new File(basePath), path).getAbsolutePath();
            }
        });

        cleanup();
    }

    @AfterAll
    public void teardown() {
        cleanup();
    }

    @Test
    public void testImportFile() throws IOException {
        final InputStream inputStream = getClass().getClassLoader().getResourceAsStream("ExcelImportServiceAbstractTest/test.xlsx");
        service.importFile(new MockMultipartFile("test", "test", "test", inputStream));

        Optional<ContactEntity> byId = getContactEntity();

        Assertions.assertTrue(byId.isPresent());
        ContactEntity contactEntity = byId.get();

        Assertions.assertEquals("test5", contactEntity.getName());
        Assertions.assertEquals("123456", contactEntity.getZip());
        Assertions.assertEquals("10.5", contactEntity.getStreet());
        Assertions.assertEquals("11.7", contactEntity.getCity());
        Assertions.assertEquals("test@test.sk", contactEntity.getContact());

        // telefon sa podla zadania meni random v sk.iway.basecms.upload.example.SpringImportService.beforeRow
        // Assertions.assertEquals("+421 907 487 001", contactEntity.getPhone());
    }

    private Optional<ContactEntity> getContactEntity() {
        return repository.findById(999L);
    }

    private void cleanup() {
        Optional<ContactEntity> byId = getContactEntity();
        byId.ifPresent(contactEntity -> repository.delete(contactEntity));
    }
}
