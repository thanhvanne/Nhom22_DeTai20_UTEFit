(() => {
    'use strict';

    const styleCharts = () => {
        if (!window.Chart || !document.body.classList.contains('uf-area-admin')) return;
        const colors = ['#e97883', '#c1c1bb', '#92928d', '#dac7a2'];
        Object.values(window.Chart.instances).forEach((chart) => {
            chart.stop();
            const isLine = chart.config.type === 'line';
            chart.options.animation = false;
            chart.options.plugins.legend.display = chart.data.datasets.length > 1;
            chart.data.datasets.forEach((dataset, index) => {
                const color = colors[index % colors.length];
                dataset.borderColor = color;
                if (isLine) {
                    dataset.tension = 0;
                    dataset.fill = false;
                    dataset.pointRadius = 0;
                    dataset.pointHoverRadius = 4;
                    dataset.borderWidth = 2;
                } else if (chart.config.type === 'bar') {
                    dataset.backgroundColor = color;
                    dataset.borderRadius = 2;
                }
            });
            Object.values(chart.options.scales || {}).forEach((axis) => {
                axis.border.display = false;
                axis.ticks.maxTicksLimit = 6;
                axis.ticks.maxRotation = 0;
            });
            chart.update('none');
        });
    };

    styleCharts();
    if (document.readyState !== 'complete') window.addEventListener('load', styleCharts, { once: true });
})();
