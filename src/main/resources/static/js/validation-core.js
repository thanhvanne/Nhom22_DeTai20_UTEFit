((root, factory) => {
    const core = factory();
    if (typeof module === 'object' && module.exports) module.exports = core;
    else root.UTEFitValidationCore = core;
})(typeof globalThis === 'object' ? globalThis : this, () => {
    'use strict';
    const normalize = (value, rule) => {
        if (typeof value !== 'string' || !rule?.normalize) return value;
        if (rule.normalize === 'email') return value.trim().toLowerCase();
        if (rule.normalize === 'name') return value.trim().replace(/\s+/gu, ' ').normalize('NFC');
        if (rule.normalize === 'mobile') return value.trim().replace(/^\+84/u, '0');
        if (rule.normalize === 'hotline') return value.trim().replace(/[ ().-]/gu, '');
        return value.trim();
    };
    const resolve = (contract, rule) => rule?.use ? { ...resolve(contract, contract.fields[rule.use]), ...rule } : rule;
    const ruleFor = (contract, path, method, name) => {
        let rule = resolve(contract, contract.fields[name] || (/Id$|^branchIds$|^beforeId$/u.test(name) ? contract.fields.id : null));
        for (const scope of contract.scopes) {
            if (new RegExp(scope.path, 'u').test(path) && (!scope.method || scope.method === method.toUpperCase()) && scope.fields[name]) {
                rule = { ...rule, ...resolve(contract, scope.fields[name]) };
            }
        }
        return rule;
    };
    const errorFor = (rule, raw) => {
        if (!rule) return '';
        const value = normalize(raw ?? '', rule);
        if (!String(value).trim()) return rule.required ? 'Vui lòng nhập hoặc chọn giá trị cho trường này.' : '';
        let invalid = /[\u0000-\u0008\u000b\u000c\u000e-\u001f\u007f]/u.test(value);
        invalid ||= rule.minLength != null && value.length < rule.minLength;
        invalid ||= rule.maxLength != null && value.length > rule.maxLength;
        invalid ||= rule.maxBytes != null && new TextEncoder().encode(value).length > rule.maxBytes;
        invalid ||= rule.pattern != null && !new RegExp(rule.pattern, 'u').test(value);
        invalid ||= rule.normalize === 'email' && value.indexOf('@') > 64;
        if (rule.type === 'integer' || rule.type === 'decimal') {
            invalid ||= value.length > 32;
            invalid ||= !(rule.type === 'integer' ? /^[0-9]+$/u : /^[0-9]+(?:\.[0-9]+)?$/u).test(value);
            if (!invalid) {
                if (rule.type === 'integer') {
                    const number = BigInt(value);
                    invalid ||= rule.min != null && number < BigInt(rule.min);
                    invalid ||= rule.max != null && number > BigInt(rule.max);
                } else {
                    const number = Number(value);
                    invalid ||= !Number.isFinite(number);
                    invalid ||= rule.min != null && number < Number(rule.min);
                    invalid ||= rule.max != null && number > Number(rule.max);
                    const fraction = value.split('.')[1]?.replace(/0+$/u, '') || '';
                    invalid ||= rule.scale != null && fraction.length > rule.scale;
                }
            }
        }
        return invalid ? rule.message || 'Giá trị không hợp lệ.' : '';
    };
    const crossErrors = (path, values) => {
        const errors = {};
        const ordered = (start, end, inclusive, message) => {
            if (values[start] && values[end] && (values[end] < values[start] || !inclusive && values[end] === values[start])) errors[end] = message;
        };
        ordered('openingTime', 'closingTime', false, 'Giờ đóng cửa phải sau giờ mở cửa.');
        ordered('startTime', 'endTime', false, 'Giờ kết thúc phải sau giờ bắt đầu.');
        ordered('startDate', 'endDate', path !== '/admin/promotions/save', 'Ngày kết thúc phải sau ngày bắt đầu.');
        ordered('from', 'to', true, 'Ngày kết thúc không được trước ngày bắt đầu.');
        ordered('fromDate', 'toDate', true, 'Ngày kết thúc không được trước ngày bắt đầu.');
        if ('confirmPassword' in values && values.confirmPassword !== (values.newPassword ?? values.password)) errors.confirmPassword = 'Mật khẩu xác nhận không khớp.';
        if (values.scheduleDate && values.startTime && /\/(create|new|edit)$/u.test(path)) {
            const start = Date.parse(`${values.scheduleDate}T${values.startTime.length === 5 ? values.startTime + ':00' : values.startTime}+07:00`);
            if (Number.isFinite(start) && start <= Date.now()) errors.scheduleDate = 'Buổi tập phải bắt đầu trong tương lai.';
        }
        return errors;
    };
    return { normalize, ruleFor, errorFor, crossErrors };
});
