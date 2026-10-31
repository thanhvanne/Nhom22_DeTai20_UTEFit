const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const base = 'http://localhost:18081';
const output = path.resolve(__dirname, '../../../target/validation-browser');
fs.mkdirSync(output, { recursive: true });
const results = [];
async function ready(page, route) {
    const response = await page.goto(base + route, { waitUntil: 'domcontentloaded' });
    assert.equal(response.status(), 200, route);
    await page.waitForFunction(() => document.documentElement.dataset.validationReady === 'true');
    await page.evaluate(() => document.fonts.ready);
}
async function login(page, role) {
    await page.context().clearCookies();
    await ready(page, '/login');
    await page.locator('input[name=email]').fill(`validation.${role}@utefit.test`);
    await page.locator('input[name=password]').fill('Validation-test-123');
    await Promise.all([page.waitForURL(url => !url.pathname.endsWith('/login')), page.locator('form[action="/login"] button[type=submit]').click()]);
}
async function checkError(page, field, form = 'form') {
    const input = page.locator(form).locator(`[name="${field}"]`).filter({visible:true}).first();
    await page.waitForFunction(element => element.getAttribute('aria-invalid') === 'true', await input.elementHandle());
    const errorId = (await input.getAttribute('aria-describedby')).split(' ').find(value => value.startsWith('ut-field-error-'));
    assert.ok(errorId, `aria-describedby ${field}`);
    const error = page.locator('#' + errorId);
    assert.ok(await error.isVisible());
    assert.ok((await error.innerText()).length > 5);
    const inputBox = await input.boundingBox();
    const errorBox = await error.boundingBox();
    assert.ok(errorBox.y >= inputBox.y + inputBox.height - 3, `Error is under ${field}`);
    const color = await error.evaluate(element => getComputedStyle(element).color);
    const border = await input.evaluate(element => getComputedStyle(element).borderTopColor);
    const channels = color.match(/[0-9]+/g).map(Number);
    assert.ok(channels[0] > channels[1] && channels[0] > channels[2], `Red error ${color}`);
    assert.equal(border, color, `Red input border ${field}`);
}
async function run(name, callback) {
    try { await callback(); results.push({name,status:'PASS'}); console.log('PASS', name); }
    catch (error) { results.push({name,status:'FAIL',error:error.message}); console.log('FAIL', name, error.message); }
    fs.writeFileSync(path.join(output,'results.json'),JSON.stringify(results,null,2));
}
(async()=>{
    const browser = await chromium.launch({channel:'chrome',headless:true});
    const context = await browser.newContext({viewport:{width:1440,height:1000},deviceScaleFactor:1.5,reducedMotion:'reduce'});
    const page = await context.newPage();
    const consoleErrors=[];
    page.on('pageerror',error=>consoleErrors.push(error.message));
    try {
        await run('Guest: blur, error under field, red border and focus first invalid',async()=>{
            await ready(page,'/register');
            await page.locator('[name=firstName]').fill('Nguyễn');
            await page.locator('[name=lastName]').fill('Thị Mai');
            await page.locator('[name=email]').fill('123123@123');
            await page.locator('[name=phone]').fill('123456789');
            await page.locator('[name=password]').fill('123');
            await page.locator('[name=confirmPassword]').fill('321');
            await page.locator('[name=email]').focus();
            await checkError(page,'phone','form[action="/register"]');
            let requests=0;
            const listener=request=>{if(request.method()==='POST'&&new URL(request.url()).pathname==='/register')requests++;};
            page.on('request',listener);
            await page.locator('form[action="/register"] button[type=submit]').click();
            await page.waitForTimeout(200);
            assert.equal(await page.evaluate(()=>document.activeElement.name),'email');
            assert.equal(requests,0);
            page.off('request',listener);
            assert.equal(await page.locator('[name=lastName]').inputValue(),'Thị Mai');
            await page.screenshot({path:path.join(output,'guest-invalid.png')});
        });
        await run('Guest: editing a touched Vietnamese name preserves word spaces',async()=>{
            const field=page.locator('[name=firstName]');
            await field.fill('Nguyễn');
            await page.locator('[name=lastName]').focus();
            await field.fill('Nguyễn ');
            await field.pressSequentially('Thị');
            assert.equal(await field.inputValue(),'Nguyễn Thị');
            await page.locator('[name=lastName]').focus();
            assert.equal(await field.getAttribute('aria-invalid'),'false');
        });
        await run('Guest: correcting fields clears errors and valid form submits to backend',async()=>{
            const email=`browser+${Date.now()}@utefit.test`;
            await page.locator('[name=email]').fill(email);
            await page.locator('[name=phone]').fill('+84901234567');
            await page.locator('[name=password]').fill('Validation-test-123');
            await page.locator('[name=confirmPassword]').fill('Validation-test-123');
            await page.locator('[name=email]').focus();
            assert.equal(await page.locator('[name=phone]').inputValue(),'0901234567');
            assert.equal(await page.locator('[name=phone]').getAttribute('aria-invalid'),'false');
            await Promise.all([page.waitForURL(url=>url.pathname==='/verify-otp'),page.locator('form[action="/register"] button[type=submit]').click()]);
        });
        await run('Guest: native form.submit bypass rejected by backend and values restored',async()=>{
            await ready(page,'/register');
            await page.locator('[name=firstName]').fill('Nguyễn');
            await page.locator('[name=lastName]').fill('Thị Kiểm Thử');
            await page.locator('[name=email]').fill('123123@123');
            await page.locator('[name=phone]').fill('123456789');
            await page.locator('[name=password]').fill('Validation-test-123');
            await page.locator('[name=confirmPassword]').fill('Validation-test-123');
            await Promise.all([page.waitForNavigation({waitUntil:'domcontentloaded'}),page.locator('form[action="/register"]').evaluate(form=>HTMLFormElement.prototype.submit.call(form))]);
            await page.waitForFunction(()=>document.documentElement.dataset.validationReady==='true');
            assert.equal(new URL(page.url()).pathname,'/register');
            assert.equal(await page.locator('[name=lastName]').inputValue(),'Thị Kiểm Thử');
            await checkError(page,'email','form[action="/register"]');
            await checkError(page,'phone','form[action="/register"]');
        });
        await run('Member: phone error follows member design and valid fields remain',async()=>{
            await login(page,'member'); await ready(page,'/member/profile');
            await page.locator('[data-profile-open=edit]').click();
            const form=page.locator('form[action="/member/profile"]');
            await form.locator('[name=phone]').fill('123456789');
            await form.locator('[name=firstName]').focus();
            await checkError(page,'phone','form[action="/member/profile"]');
            assert.equal(await form.locator('[name=firstName]').inputValue(),'Nguyễn');
            await page.screenshot({path:path.join(output,'member-invalid.png')});
        });
        await run('PT: experience rejects negative and noninteger values under correct field',async()=>{
            await login(page,'pt'); await ready(page,'/pt/profile/edit');
            const form='form[data-staff-profile-form="information"]';
            await page.locator(form+' [name=yearsExperience]').fill('-1');
            await page.locator(form+' [name=specialization]').focus();
            await checkError(page,'yearsExperience',form);
            await page.locator(form+' [name=yearsExperience]').fill('1.5');
            await page.locator(form+' [name=specialization]').focus();
            await checkError(page,'yearsExperience',form);
            await page.screenshot({path:path.join(output,'pt-invalid.png')});
        });
        await run('Manager: invalid email receives inline error',async()=>{
            await login(page,'manager'); await ready(page,'/manager');
            await page.locator('form[action="/manager/checkins"] [name=email]').fill('123123@123');
            await page.locator('form[action="/manager/checkins"] button[type=submit]').click();
            await checkError(page,'email','form[action="/manager/checkins"]');
            await page.screenshot({path:path.join(output,'manager-invalid.png')});
        });
        await run('Admin: branch hotline differs from mobile and date/time errors are inline',async()=>{
            await login(page,'admin'); await ready(page,'/admin/branches/create');
            const form='form[method=post]:has([name=openingTime])';
            await page.locator(form+' [name=name]').fill('Chi nhánh kiểm thử');
            await page.locator(form+' [name=address]').fill('Thủ Đức');
            await page.locator(form+' [name=phone]').fill('028 0000 1001');
            await page.locator(form+' [name=openingTime]').fill('22:00');
            await page.locator(form+' [name=closingTime]').fill('06:00');
            await page.locator(form+' [name=name]').focus();
            await checkError(page,'closingTime',form);
            assert.equal(await page.locator(form+' [name=phone]').getAttribute('aria-invalid'),'false');
            await page.screenshot({path:path.join(output,'admin-invalid.png')});
        });
        await run('Admin: required branch fields reject empty submit and focus name',async()=>{
            await ready(page,'/admin/branches/create');
            const form='form[method=post]:has([name=openingTime])';
            await page.locator(form+' button[type=submit]').click();
            await checkError(page,'name',form);
            await checkError(page,'address',form);
            assert.equal(await page.evaluate(()=>document.activeElement.name),'name');
        });
        await run('Mobile: errors stay below inputs without horizontal overflow',async()=>{
            await page.setViewportSize({width:390,height:844});
            await ready(page,'/register');
            await page.locator('[name=email]').fill('123123@123');
            await page.locator('[name=phone]').focus();
            await checkError(page,'email','form[action="/register"]');
            assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1));
            await page.screenshot({path:path.join(output,'mobile-invalid.png'),fullPage:true});
        });
        fs.writeFileSync(path.join(output,'console-errors.json'),JSON.stringify(consoleErrors,null,2));
    } finally { await browser.close(); }
    console.log('RESULT',results.filter(result=>result.status==='PASS').length,'PASS',results.filter(result=>result.status==='FAIL').length,'FAIL');
    if(results.some(result=>result.status==='FAIL'))process.exitCode=1;
})().catch(error=>{console.error(error);process.exit(1)});
