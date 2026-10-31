const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const read = file => fs.readFileSync('src/main/resources/'+file,'utf8');
const directory = read('templates/fragments/public-directory.html');

test('premium directory is anonymous-only, retaining original role-specific templates', () => {
    for (const name of ['trainers','classes','branches','membership-plans','trainer-detail','branch-detail']) {
        const template = read('templates/guest/'+name+'.html');
        assert.match(template, /th:if="\$\{#authorization.expression\('isAnonymous\(\)'\)\}">\s*<th:block th:replace="~\{fragments\/public-directory :: assets\}/);
        assert.match(template, /<th:block th:unless="\$\{#authorization.expression\('isAnonymous\(\)'\)\}">\s*<main/);
    }
});

test('Guest navigation reuses approved homepage fragment and stylesheet', () => {
    const header = read('templates/fragments/header.html');
    assert.match(header, /fragments\/public-cinematic :: navigation/);
    assert.match(header, /utefit-public-cinematic.css/);
    assert.match(header, /<header th:unless="\$\{#authorization.expression\('isAnonymous\(\)'\)\}"/);
});

test('all four shared catalog routes select the established Member Navy layout', () => {
    for (const name of ['trainers','classes','branches','membership-plans']) {
        const template=read('templates/guest/'+name+'.html');
        for(const fragment of ['head','header','footer','scripts']) assert.ok(template.includes('member/fragments/layout :: '+fragment),name+' '+fragment);
        assert.ok(template.includes('uf-member uf-member-discovery'));
    }
});

test('all search forms preserve GET endpoints and backend field names', () => {
    for (const route of ['trainers','classes','branches','membership-plans']) assert.match(directory,new RegExp('th:action="@\\{/'+route+'\\}" method="get"'));
    assert.match(directory,/name="keyword"/);
    assert.match(directory,/id="keyword"|searchField\('keyword'/);
    assert.match(directory,/id="branchId" name="branchId"/);
    assert.match(directory,/searchField\('branch-search'/);
    assert.doesNotMatch(directory,/method="post"|fetch\(/i);
});

test('plans retain actual terms, benefits, price, branch scope and plan-specific checkout entry', () => {
    for (const value of ['p.durationDescription','p.benefit','p.accessScope','p.branchIds','money(p.price)','planId=${p.id}','planPage.totalPages']) assert.ok(directory.includes(value),value);
    assert.doesNotMatch(directory,/Phổ biến nhất|Tốt nhất|giai đoạn 4/);
});

test('branch media uses the model URL and labels demo imagery without an approval field', () => {
    const media = read('templates/fragments/branch-media.html');
    assert.doesNotMatch(directory,/branch.imageUrl|b.imageUrl/);
    assert.match(directory,/fragments\/branch-media :: publicMedia/);
    assert.match(media,/Ảnh minh họa/);
    assert.match(media,/th:src="\$\{sourceUrl\}"/);
    assert.match(media,/hasImage=\$\{validUrl and !foreignGym\}/);
    assert.doesNotMatch(media,/chưa xác minh|imageVerified|verificationStatus/);
    for (const value of ['branch.openingTime','branch.closingTime','branchReviews.latest','review.imageUrl','review.videoUrl']) assert.ok(directory.includes(value));
    for (const name of ['branches','branch-detail']) {
        const template=read('templates/guest/'+name+'.html');
        assert.doesNotMatch(template,/th:src="\$\{(?:b|branch)\.imageUrl\}"/);
        assert.match(template,/fragments\/branch-media :: publicMedia/);
    }
});

test('classes distinguish capacity from availability and use existing member schedule flow', () => {
    assert.match(directory,/@\{\/member\/classes\}/);
    assert.match(directory,/c.status == 'FULL'/);
    assert.match(directory,/Sức chứa không biểu thị số chỗ còn lại/);
    assert.doesNotMatch(directory,/@\{\/classes\/\{id\}/);
});

test('progressive enhancement remains visible without JS, motion uses core transforms only', () => {
    const motion=read('static/js/utefit-public-directory.js');
    const styles=read('static/css/utefit-public-directory.css');
    assert.match(motion,/prefers-reduced-motion: no-preference/);
    assert.match(motion,/timeline\.revert\(\)/);
    assert.match(motion,/media\.revert\(\)/);
    assert.doesNotMatch(motion,/ScrollTrigger|pin:|preventDefault|\.innerHTML|fetch\(/);
    assert.match(styles,/prefers-reduced-motion: reduce/);
    assert.match(styles,/img\[hidden\]/);
    assert.doesNotMatch(styles,/\.pd-page\s*\{[^}]*opacity:\s*0/s);
});
