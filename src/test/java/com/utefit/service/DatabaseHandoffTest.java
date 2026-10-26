package com.utefit.service;

import com.utefit.entity.*;
import com.utefit.enums.*;
import com.utefit.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.config.import=", "spring.datasource.url=jdbc:h2:mem:handoff_accounts;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS dbo",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.hikari.schema=DBO", "spring.jpa.properties.hibernate.default_schema=DBO"})
@Transactional
class DatabaseHandoffTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired AuthService auth;
    @Autowired BranchRepository branches;
    @Autowired TrainerRepository trainers;
    @Autowired ManagerBranchAssignmentRepository assignments;
    @Autowired ManagerAccessService managers;
    @Autowired WebApplicationContext context;
    private String seed;

    @BeforeEach void insertOnlyPortableAccountFixtureIntoH2() throws Exception {
        seed = Files.readString(Path.of("database/DB_Data_2.sql"));
        String accountSql = seed.substring(seed.indexOf("INSERT INTO dbo.users("), seed.indexOf("-- END DEMO USERS"));
        jdbc.execute(accountSql);
    }

    @Test void exactlyFiftyFiveAccountsHaveValidRealEncoderHashesAndAuthenticate() {
        assertEquals(55, users.count());
        for (var roleCount : Map.of(UserRole.ADMIN, 10, UserRole.MEMBER, 20, UserRole.PT, 20, UserRole.MANAGER, 5).entrySet()) {
            var accounts = users.findAll().stream().filter(user -> user.getRole() == roleCount.getKey()).toList();
            assertEquals(roleCount.getValue(), accounts.size());
            for (var account : accounts) {
                assertEquals(UserStatus.ACTIVE, account.getStatus()); assertTrue(account.getEnabled());
                assertTrue(encoder.matches("123456789", account.getPassword()));
                assertEquals(account.getId(), auth.authenticate(account.getEmail(), "123456789").getId());
                assertTrue(account.getPhone().matches("0[0-9]{9}"));
            }
        }
        assertEquals(0, jdbc.queryForObject("select count(*) from otp_codes", Integer.class));
    }

    @Test void actualLoginAndJwtWorkForAllFourRolesAndManagersHaveOnlyTheirBranch() throws Exception {
        var branchRows = new ArrayList<Branch>();
        for (int index = 1; index <= 5; index++) {
            var branch = branches.saveAndFlush(Branch.builder().name("Demo branch " + index).address("Development only").active(true).build());
            branchRows.add(branch);
            var manager = users.findByEmail("manager%02d@utefit.test".formatted(index)).orElseThrow();
            assignments.saveAndFlush(ManagerBranchAssignment.builder().manager(manager).branch(branch).build());
        }
        for (int index = 1; index <= 20; index++) {
            var trainer = users.findByEmail("pt%02d@utefit.test".formatted(index)).orElseThrow();
            trainers.saveAndFlush(Trainer.builder().user(trainer).branch(branchRows.get((index - 1) / 4)).active(true).build());
        }
        var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        for (var entry : Map.of("admin01", "/admin/dashboard", "member01", "/member/dashboard", "pt01", "/pt/dashboard", "manager01", "/manager").entrySet()) {
            var response = mvc.perform(post("/login").with(csrf()).param("email", entry.getKey() + "@utefit.test").param("password", "123456789"))
                    .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(entry.getValue())).andReturn().getResponse();
            String cookieHeader = response.getHeader("Set-Cookie");
            assertNotNull(cookieHeader); assertTrue(cookieHeader.contains("HttpOnly"));
            String token = cookieHeader.substring(cookieHeader.indexOf('=') + 1, cookieHeader.indexOf(';'));
            mvc.perform(get(entry.getValue()).cookie(new jakarta.servlet.http.Cookie("UTEFIT_AUTH", token)))
                    .andExpect(status().isOk());
        }
        for (int index = 1; index <= 5; index++) {
            String email = "manager%02d@utefit.test".formatted(index);
            Long allowed = branchRows.get(index - 1).getId();
            Long denied = branchRows.get(index % 5).getId();
            assertEquals(allowed, managers.requireBranch(email, allowed, false).getId());
            assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> managers.requireBranch(email, denied, false));
        }
    }

    @Test void unverifiedAccountStillCannotLoginAndDemoDoesNotDisableOtpRules() {
        var account = users.findByEmail("member01@utefit.test").orElseThrow();
        account.setStatus(UserStatus.UNVERIFIED); account.setEnabled(false); users.saveAndFlush(account);
        assertThrows(org.springframework.security.core.AuthenticationException.class, () -> auth.authenticate(account.getEmail(), "123456789"));
    }

    @Test void schemaContainsExactReviewedMigrationsAndBothScriptsFailClosed() throws Exception {
        String schema = Files.readString(Path.of("database/DB_Khung_1.sql"));
        assertTrue(schema.contains("DECLARE @AllowReset bit = 0"));
        assertTrue(schema.indexOf("IF @AllowReset") < schema.indexOf("DROP DATABASE"));
        assertFalse(java.util.regex.Pattern.compile("(?im)^\\s*GO\\s*$").matcher(schema).find());
        assertFalse(schema.contains(":r "));
        try (var paths = Files.list(Path.of("database/migrations"))) {
            for (var path : paths.filter(path -> path.getFileName().toString().matches("V7\\d\\d__.*\\.sql")).toList()) {
                String name = path.getFileName().toString();
                String embedded = schema.substring(schema.indexOf("-- BEGIN EMBEDDED " + name), schema.indexOf("-- END EMBEDDED " + name)).replace("''", "'");
                assertTrue(embedded.replace("\r\n", "\n").contains(Files.readString(path).strip().replace("\r\n", "\n")), name);
            }
        }
        assertTrue(seed.contains("DEVELOPMENT_ONLY")); assertTrue(seed.contains("sp_getapplock"));
        assertTrue(seed.contains("UTEFit.DemoSeed.Version"));
        assertFalse(java.util.regex.Pattern.compile("(?i)INSERT\\s+(?:INTO\\s+)?dbo\\.sepay_").matcher(seed).find());
        assertFalse(seed.contains("UTEFitLiveDemo;"));
    }
}
