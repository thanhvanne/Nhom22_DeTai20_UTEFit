/**
 * UTEFit - Trainers Page
 *
 * Features:
 * - Scroll reveal animation
 * - Smooth scrolling
 * - Respect reduced-motion preference
 */

(function () {
    "use strict";

    document.addEventListener("DOMContentLoaded", function () {

        initScrollReveal();

        initSmoothScroll();

    });


    /**
     * Scroll Reveal Animation
     */
    function initScrollReveal() {

        const elements = document.querySelectorAll(
            ".reveal-on-scroll"
        );

        if (elements.length === 0) {
            return;
        }

        const prefersReducedMotion = window.matchMedia(
            "(prefers-reduced-motion: reduce)"
        ).matches;

        if (prefersReducedMotion) {
            return;
        }

        if (!("IntersectionObserver" in window)) {
            return;
        }

        const observer = new IntersectionObserver(
            function (entries, currentObserver) {

                entries.forEach(function (entry) {

                    if (entry.isIntersecting) {

                        entry.target.classList.add("visible");

                        currentObserver.unobserve(entry.target);

                    }

                });

            },
            {
                threshold: 0.08,
                rootMargin: "0px 0px 40px 0px"
            }
        );

        elements.forEach(function (element) {

            // Only hide elements when JavaScript is available.
            element.classList.add("will-animate");

            observer.observe(element);

        });

    }


    /**
     * Smooth Scroll
     */
    function initSmoothScroll() {

        const links = document.querySelectorAll(
            'a[href^="#"]'
        );

        links.forEach(function (link) {

            link.addEventListener("click", function (event) {

                const targetId = link.getAttribute("href");

                if (!targetId || targetId === "#") {
                    return;
                }

                const targetElement = document.querySelector(
                    targetId
                );

                if (!targetElement) {
                    return;
                }

                event.preventDefault();

                const prefersReducedMotion = window.matchMedia(
                    "(prefers-reduced-motion: reduce)"
                ).matches;

                targetElement.scrollIntoView({
                    behavior: prefersReducedMotion
                        ? "auto"
                        : "smooth",

                    block: "start"
                });

            });

        });

    }

})();