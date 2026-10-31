package com.utefit.controller;

import com.utefit.controller.auth.AuthController;
import com.utefit.security.JwtService;
import com.utefit.service.AuthService;
import com.utefit.validation.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FormValidationMvcTest {
    private MockMvc mvc;
    private AuthService auth;
    private Probe probe;

    @BeforeEach void setup() {
        auth = mock(AuthService.class); probe = new Probe();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean(); validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(auth,mock(JwtService.class),false),probe)
                .setControllerAdvice(new FormValidationAdvice())
                .addInterceptors(new FormValidationInterceptor(new ImageValidator(),new VideoValidator()))
                .setValidator(validator).build();
    }

    @ParameterizedTest
    @CsvSource({"email,123123@123","phone,123456789","phone,0000000000","firstName,12345","password,123","confirmPassword,wrong-confirmation"})
    void rawPostCannotBypassFrontend(String field, String invalid) throws Exception {
        var request = post("/register").param("firstName","Nguyễn").param("lastName","Thị Mai")
                .param("email","test+tag@utefit.test").param("phone","0901234567")
                .param("password","Validation-test-123").param("confirmPassword","Validation-test-123");
        request.with(servletRequest -> { servletRequest.setParameter(field,invalid); return servletRequest; });
        var result = mvc.perform(request).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/register"))
                .andReturn();
        assertTrue(((Map<?,?>)result.getFlashMap().get("validationErrors")).containsKey(field));
        assertEquals("Thị Mai",((Map<?,?>)result.getFlashMap().get("validationValues")).get("lastName"));
        assertFalse(((Map<?,?>)result.getFlashMap().get("validationValues")).containsKey("password"));
        verifyNoInteractions(auth);
    }

    @Test void validPlusEmailAndVietnameseNamesReachController() throws Exception {
        when(auth.register(any())).thenReturn("test+tag@utefit.test");
        mvc.perform(post("/register").param("firstName","Nguyễn").param("lastName","Thị Mai")
                .param("email"," test+tag@utefit.test ").param("phone","+84901234567")
                .param("password","Validation-test-123").param("confirmPassword","Validation-test-123"))
                .andExpect(status().is3xxRedirection());
        verify(auth).register(argThat(form -> form.getEmail().equals("test+tag@utefit.test") && form.getPhone().equals("0901234567") && form.getFirstName().equals("Nguyễn")));
    }

    @ParameterizedTest
    @CsvSource({"/admin/classes/create,capacity,-1","/admin/classes/create,capacity,1.5","/member/trainer-requests,trainerId,-1","/admin/promotions/save,discountPercent,100","/pt/profile/edit,yearsExperience,71","/manager/checkins,email,123123@123"})
    void everyRoleRejectsInvalidRawInputAsFieldJson(String path,String field,String invalid) throws Exception {
        mvc.perform(post(path).accept("application/json").param(field,invalid))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors."+field).exists());
        assertEquals(0,probe.calls.get());
    }

    @Test void missingRequiredAndOverlengthFieldsReturnFieldJson() throws Exception {
        mvc.perform(post("/register").accept("application/json"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.email").exists());
        mvc.perform(post("/member/trainer-requests").accept("application/json").param("trainerId","1").param("message","x".repeat(2001)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.message").exists());
        assertEquals(0,probe.calls.get());
    }

    @Test void typeConversionFailureReturnsVietnameseFieldJson() throws Exception {
        mvc.perform(post("/validation/type").accept("application/json").param("count","not-an-integer"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.count").exists());
        assertEquals(0,probe.calls.get());
    }

    @Controller
    @Profile("validation-unit")
    static class Probe {
        final AtomicInteger calls = new AtomicInteger();
        @PostMapping({"/admin/classes/create","/member/trainer-requests","/admin/promotions/save","/pt/profile/edit","/manager/checkins"})
        @ResponseBody public Map<String,Boolean> save() { calls.incrementAndGet(); return Map.of("saved",true); }
        @PostMapping("/validation/type")
        @ResponseBody public Map<String,Boolean> typed(@RequestParam int count) { calls.incrementAndGet(); return Map.of("saved",true); }
    }
}
