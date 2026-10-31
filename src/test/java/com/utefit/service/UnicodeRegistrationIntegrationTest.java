package com.utefit.service;

import com.utefit.enums.UserStatus;
import com.utefit.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class UnicodeRegistrationIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired EntityManager entityManager;
    @MockitoBean MailService mail;
    private MockMvc mvc;

    @BeforeEach void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilter(context.getBean(CharacterEncodingFilter.class))
                .apply(springSecurity()).build();
    }

    @ParameterizedTest
    @CsvSource({"Nguyễn,Văn", "Trần,Thị Mỹ Linh", "Đặng,Quốc Huy", "Đỗ,Minh Đức"})
    void registrationOtpAndMemberViewsPreserveVietnameseNames(String firstName, String lastName) throws Exception {
        String email = UUID.randomUUID() + "@unicode.test";
        String fullName = firstName + " " + lastName;
        mvc.perform(post("/register").with(csrf()).characterEncoding(StandardCharsets.UTF_8)
                        .param("firstName", firstName).param("lastName", lastName)
                        .param("email", email).param("phone", "0901234567")
                        .param("password", "Unicode-test-1234").param("confirmPassword", "Unicode-test-1234"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/verify-otp?email=" + email.replace("@", "%40")));
        entityManager.clear();
        var registered = users.findByEmail(email).orElseThrow();
        assertEquals(firstName, registered.getFirstName());
        assertEquals(lastName, registered.getLastName());
        assertEquals(UserStatus.UNVERIFIED, registered.getStatus());
        var code = ArgumentCaptor.forClass(String.class);
        verify(mail).sendOtp(eq(email), code.capture(), eq("đăng ký"));
        mvc.perform(post("/verify-otp").with(csrf()).param("email", email).param("code", code.getValue()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        entityManager.flush();
        entityManager.clear();
        var verified = users.findByEmail(email).orElseThrow();
        assertEquals(UserStatus.ACTIVE, verified.getStatus());
        assertTrue(verified.getEnabled());
        assertEquals(fullName, verified.getFullName());
        for (String route : new String[]{"/member/dashboard", "/member/profile"}) {
            var response = mvc.perform(get(route).with(user(email).roles("MEMBER")))
                    .andExpect(status().isOk()).andExpect(content().encoding("UTF-8"))
                    .andReturn().getResponse();
            assertTrue(response.getContentAsString(StandardCharsets.UTF_8).contains(fullName), route);
        }
        mvc.perform(post("/member/profile").with(user(email).roles("MEMBER")).with(csrf())
                        .param("firstName", firstName).param("lastName", lastName).param("phone", "0901234567"))
                .andExpect(status().is3xxRedirection());
        entityManager.flush();
        entityManager.clear();
        assertEquals(fullName, users.findByEmail(email).orElseThrow().getFullName());
    }
}
