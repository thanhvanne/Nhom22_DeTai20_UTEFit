const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const core = require('../../main/resources/static/js/validation-core.js');
const contract = JSON.parse(fs.readFileSync('src/main/resources/static/js/validation-rules.json', 'utf8'));
const rule = (name, path = '/register', method = 'POST') => core.ruleFor(contract, path, method, name);

for (const email of ['123123@123', 'abc@', '@gmail.com', 'a b@example.com', 'a@example', 'a..b@example.com', 'a@-example.com']) {
    test(`rejects invalid email ${email}`, () => assert.ok(core.errorFor(rule('email'), email)));
}
for (const email of ['test@example.com', 'test+tag@utefit.test', 'first.last@sub.example.com']) {
    test(`accepts email ${email}`, () => assert.equal(core.errorFor(rule('email'), email), ''));
}
for (const phone of ['123456789', '0000000000', '090123456', '09012345678', '0123456789', '090abcdefg', '0951234567']) {
    test(`rejects invalid mobile ${phone}`, () => assert.ok(core.errorFor(rule('phone'), phone)));
}
for (const phone of ['0321234567', '0521234567', '0701234567', '0811234567', '0901234567', '+84901234567']) {
    test(`accepts mobile ${phone}`, () => assert.equal(core.errorFor(rule('phone'), phone), ''));
}
for (const name of ['Nguyễn', 'Trần Thị Mỹ Linh', "O'Connor", 'Jean-Luc']) {
    test(`accepts Unicode name ${name}`, () => assert.equal(core.errorFor(rule('firstName'), name), ''));
}
for (const name of ['123', '!!!', 'Nguyễn123', '<script>']) {
    test(`rejects invalid name ${name}`, () => assert.ok(core.errorFor(rule('firstName'), name)));
}
for (const number of ['-1', '1.5', 'NaN', 'Infinity', '2147483648']) {
    test(`rejects invalid integer ${number}`, () => assert.ok(core.errorFor(rule('capacity'), number)));
}
test('distinguishes hotline and mobile', () => {
    assert.equal(core.errorFor(rule('phone', '/admin/branches/create'), '028 0000 1001'), '');
    assert.ok(core.errorFor(rule('phone'), '028 0000 1001'));
});
test('keeps existing manager and login password policies', () => {
    assert.ok(core.errorFor(rule('password', '/admin/managers'), 'short123'));
    assert.equal(core.errorFor(rule('password', '/login'), 'old123'), '');
});
test('enforces confirmation and date/time ordering', () => {
    assert.ok(core.crossErrors('/register', { password: 'correct-123', confirmPassword: 'different' }).confirmPassword);
    assert.ok(core.crossErrors('/validation', { startTime: '11:00', endTime: '10:00' }).endTime);
    assert.ok(core.crossErrors('/validation', { startDate: '2099-01-03', endDate: '2099-01-01' }).endDate);
});
test('enforces UTF-8 password limit and text lengths', () => {
    assert.equal(core.errorFor(rule('password'), 'é'.repeat(36)), '');
    assert.ok(core.errorFor(rule('password'), 'é'.repeat(37)));
    assert.ok(core.errorFor(rule('comment'), 'x'.repeat(2001)));
});
test('checks large IDs precisely and preserves allowed plus sign', () => {
    assert.equal(core.errorFor(rule('branchId'), '9223372036854775807'), '');
    assert.ok(core.errorFor(rule('branchId'), '9223372036854775808'));
    assert.equal(core.normalize(' A+B@Example.com ', rule('email')), 'a+b@example.com');
});
