package com.utefit.validation;

import com.utefit.dto.auth.RegisterRequest;
import com.utefit.dto.member.PasswordChangeRequest;
import com.utefit.dto.admin.BranchFormDto;
import com.utefit.dto.admin.ClassScheduleFormDto;
import com.utefit.service.impl.AuthServiceImpl;
import com.utefit.repository.UserRepository;
import com.utefit.service.OtpService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InputRulesTest {
    @ParameterizedTest
    @ValueSource(strings={"123123@123","abc@","@gmail.com","a b@example.com","a@example","a@-example.com","a..b@example.com","a@example..com"})
    void rejectsInvalidEmail(String email) {
        assertNotNull(InputRules.error("email",email));
    }

    @ParameterizedTest
    @ValueSource(strings={"ten@example.com","ten+tag@example.com","member02@utefit.test","first.last@sub.example.com","  User+tag@Example.com  "})
    void acceptsSupportedEmail(String email) {
        assertNull(InputRules.error("email",email));
    }

    @ParameterizedTest
    @ValueSource(strings={"123456789","0000000000","090123456","09012345678","0123456789","090abcdefg","090 1234567","0311234567","0951234567"})
    void rejectsInvalidMobile(String phone) {
        assertNotNull(InputRules.error("phone",phone));
    }

    @ParameterizedTest
    @ValueSource(strings={"0321234567","0521234567","0551234567","0701234567","0811234567","0901234567","0981234567","+84901234567"})
    void acceptsValidMobile(String phone) {
        assertNull(InputRules.error("phone",phone));
    }

    @ParameterizedTest
    @ValueSource(strings={"Nguyễn","Trần Thị Mỹ Linh","Đỗ Quốc Huy","Jean-Luc","O'Connor","D’Arcy"})
    void acceptsUnicodeNames(String name) {
        assertNull(InputRules.error("personalName",name));
    }

    @ParameterizedTest
    @ValueSource(strings={"12345","Nguyễn123","!!!","<script>","Nguyễn @ An"})
    void rejectsInvalidNames(String name) {
        assertNotNull(InputRules.error("personalName",name));
    }

    @ParameterizedTest
    @ValueSource(strings={"-1","1.5","NaN","Infinity","2147483648","1e3"})
    void rejectsNonIntegerOrOverflowCapacity(String value) {
        assertNotNull(InputRules.error("capacity",value));
    }

    @ParameterizedTest
    @ValueSource(strings={"-1","0","1.25","10000000000000","NaN","Infinity","1e5"})
    void rejectsInvalidMoney(String value) {
        assertNotNull(InputRules.error("price",value));
    }

    @Test void preservesPlusAndNormalizesMobileAndNames() {
        assertEquals("a+b@utefit.test",InputRules.normalize(" A+B@utefit.test ",InputRules.rule("email")));
        assertEquals("0901234567",InputRules.normalize("+84901234567",InputRules.rule("phone")));
        assertEquals("Trần Thị Mai",InputRules.normalize("  Trần   Thị Mai  ",InputRules.rule("personalName")));
        assertNull(InputRules.error("hotline","028 0000 1001"));
        assertNull(InputRules.error("hotline","19001234"));
        assertNotNull(InputRules.error("phone","028 0000 1001"));
    }

    @Test void checksPasswordBytesAndConfirmationAtBeanValidation() {
        assertNull(InputRules.error("password","é".repeat(36)));
        assertNotNull(InputRules.error("password","é".repeat(37)));
        PasswordChangeRequest form = new PasswordChangeRequest();
        form.setCurrentPassword("old-password"); form.setNewPassword("new-password"); form.setConfirmPassword("other-password");
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(form).stream().anyMatch(error -> error.getPropertyPath().toString().equals("confirmPassword")));
        }
    }

    @Test void rejectsReversedAndInvalidSqlDates() {
        assertTrue(RequestChecks.errors("/admin/promotions/save","POST",Map.of("startDate",new String[]{"2099-06-02"},"endDate",new String[]{"2099-06-01"})).containsKey("endDate"));
        assertTrue(RequestChecks.errors("/validation","POST",Map.of("scheduleDate",new String[]{"2025-02-30"})).containsKey("scheduleDate"));
        assertTrue(RequestChecks.errors("/validation","POST",Map.of("startDate",new String[]{"0000-01-01"})).containsKey("startDate"));
        assertTrue(RequestChecks.errors("/validation","POST",Map.of("startTime",new String[]{"10:00"},"endTime",new String[]{"09:00"})).containsKey("endTime"));
    }

    @Test void validatesTimesInsideBeans() {
        BranchFormDto branch = BranchFormDto.builder().name("Chi nhánh 1").address("Thủ Đức").openingTime(LocalTime.of(22,0)).closingTime(LocalTime.of(6,0)).build();
        assertThrows(InputViolationException.class,()->FormChecks.validate(branch));
        ClassScheduleFormDto schedule = ClassScheduleFormDto.builder().fitnessClassId(1L).scheduleDate(LocalDate.now().minusDays(1)).startTime(LocalTime.NOON).endTime(LocalTime.of(13,0)).build();
        assertThrows(InputViolationException.class,()->FormChecks.validate(schedule));
    }

    @Test void enforcesRequiredAndLengthWithoutBreakingOptionalSearch() {
        assertTrue(RequestChecks.errors("/register","POST",Map.of()).containsKey("email"));
        assertFalse(RequestChecks.errors("/admin/managers","GET",Map.of()).containsKey("email"));
        assertNotNull(InputRules.error("email","x".repeat(101)+"@example.com"));
        assertNotNull(InputRules.error("comment","x".repeat(2001)));
        assertNotNull(InputRules.error("id","9223372036854775808"));
        assertNull(InputRules.error("id","9223372036854775807"));
    }

    @Test void rejectsInvalidRegistrationBeforeAnyRepositoryOrOtpCall() {
        var users = mock(UserRepository.class);
        var otp = mock(OtpService.class);
        var service = new AuthServiceImpl(users,mock(org.springframework.security.crypto.password.PasswordEncoder.class),otp,mock(org.springframework.security.authentication.AuthenticationManager.class));
        RegisterRequest request = validRegistration();
        request.setEmail("123123@123"); request.setPhone("123456789");
        assertThrows(InputViolationException.class,()->service.register(request));
        verifyNoInteractions(users,otp);
    }

    static RegisterRequest validRegistration() {
        RegisterRequest form = new RegisterRequest();
        form.setFirstName("Nguyễn"); form.setLastName("Thị Mai"); form.setEmail("test+validation@utefit.test");
        form.setPhone("0901234567"); form.setPassword("Validation-test-123"); form.setConfirmPassword("Validation-test-123");
        return form;
    }
}
