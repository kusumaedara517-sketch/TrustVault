package com.snovasys.trustvault;

import java.nio.charset.StandardCharsets;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import com.snovasys.trustvault.document.Document;
import com.snovasys.trustvault.document.DocumentRepository;
import com.snovasys.trustvault.document.DocumentService;
import com.snovasys.trustvault.user.Role;
import com.snovasys.trustvault.user.User;
import com.snovasys.trustvault.user.UserRepository;

@SpringBootTest
class DocumentSecurityTest {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DataSource dataSource;

    private User owner;
    private User otherUser;

    @BeforeEach
    void setup() throws Exception {

        String databaseUrl =
                dataSource.getConnection()
                        .getMetaData()
                        .getURL();

        assertTrue(
                databaseUrl.startsWith("jdbc:h2:"),
                "SECURITY: Tests must use H2, not the real PostgreSQL database!"
        );

        documentRepository.deleteAll();
        userRepository.deleteAll();

        owner = new User();
        owner.setName("Security Owner");
        owner.setEmail("security-owner@test.com");
        owner.setPasswordHash("test-password");
        owner.setRole(Role.VIEWER);
        owner = userRepository.save(owner);

        otherUser = new User();
        otherUser.setName("Other User");
        otherUser.setEmail("security-other@test.com");
        otherUser.setPasswordHash("test-password");
        otherUser.setRole(Role.VIEWER);
        otherUser = userRepository.save(otherUser);
    }

    @Test
    void userCannotDownloadAnotherUsersDocument()
            throws Exception {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "private.txt",
                        "text/plain",
                        "TOP SECRET"
                                .getBytes(StandardCharsets.UTF_8)
                );

        Document document =
                documentService.upload(
                        file,
                        "Private",
                        owner.getEmail()
                );

        assertThrows(
                SecurityException.class,
                () -> documentService.download(
                        document.getId(),
                        otherUser.getEmail()
                )
        );
    }
}