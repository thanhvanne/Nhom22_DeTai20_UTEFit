(function () {
    'use strict';
    const navigation = document.querySelector('.pc-nav');
    if (!navigation) return;
    const main = document.querySelector('main');
    const skip = navigation.querySelector('.pc-skip');
    if (main && skip) {
        if (!main.id) main.id = 'utefit-content';
        skip.href = '#' + main.id;
        if (!main.hasAttribute('tabindex')) main.tabIndex = -1;
    }
    navigation.querySelectorAll('nav a').forEach(link => {
        if (new URL(link.href).pathname === location.pathname) link.setAttribute('aria-current', 'page');
    });
    const menu = navigation.querySelector('.pc-menu');
    if (!menu) return;
    menu.addEventListener('keydown', event => {
        if (event.key === 'Escape' && menu.open) {
            menu.open = false;
            menu.querySelector('summary').focus();
        }
    });
    document.addEventListener('click', event => {
        if (menu.open && !menu.contains(event.target)) menu.open = false;
    });
}());
