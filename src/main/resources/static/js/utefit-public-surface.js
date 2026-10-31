const UTEFitSurface = (() => {
    'use strict';

    const clamp = (value, lower = 0, upper = 1) => Math.min(upper, Math.max(lower, value));
    const mix = (start, end, fraction) => start + (end - start) * fraction;
    const smooth = (start, end, value) => {
        const fraction = clamp((value - start) / (end - start));
        return fraction * fraction * (3 - 2 * fraction);
    };
    const randomGenerator = () => {
        let seed = 2902026;
        return () => {
            seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
            return seed / 4294967296;
        };
    };
    const createRankMap = (size, target = false) => {
        const random = randomGenerator();
        const grid = Array.from({ length: 33 * 33 }, random);
        const noise = (horizontal, vertical, frequency) => {
            const cellX = horizontal * frequency;
            const cellY = vertical * frequency;
            const column = Math.floor(cellX);
            const row = Math.floor(cellY);
            const blendX = smooth(0, 1, cellX - column);
            const blendY = smooth(0, 1, cellY - row);
            return mix(mix(grid[row * 33 + column], grid[row * 33 + column + 1], blendX),
                mix(grid[(row + 1) * 33 + column], grid[(row + 1) * 33 + column + 1], blendX), blendY);
        };
        const values = new Float32Array(size * size);
        let minimum = Infinity;
        let maximum = -Infinity;
        for (let row = 0; row < size; row++) {
            for (let column = 0; column < size; column++) {
                const horizontal = column / size;
                const vertical = row / size;
                const direction = target ? 1 - vertical : 1 - horizontal;
                const value = direction * .52 + noise(horizontal, vertical, 5) * .27
                    + noise(horizontal, vertical, 15) * .14 + noise(horizontal, vertical, 31) * .07;
                values[row * size + column] = value;
                minimum = Math.min(minimum, value);
                maximum = Math.max(maximum, value);
            }
        }
        const pixels = new Uint8Array(size * size * 4);
        values.forEach((value, index) => {
            const rank = Math.round((.025 + (value - minimum) / (maximum - minimum) * .95) * 255);
            pixels.set([rank, rank, rank, 255], index * 4);
        });
        return { size, pixels };
    };
    const rankAt = (map, horizontal, vertical) => {
        const sampleX = clamp(horizontal * map.size - .5, 0, map.size - 1);
        const sampleY = clamp(vertical * map.size - .5, 0, map.size - 1);
        const column = Math.floor(sampleX);
        const row = Math.floor(sampleY);
        const read = (offsetX, offsetY) => map.pixels[(Math.min(row + offsetY, map.size - 1) * map.size
            + Math.min(column + offsetX, map.size - 1)) * 4] / 255;
        return mix(mix(read(0, 0), read(1, 0), sampleX - column), mix(read(0, 1), read(1, 1), sampleX - column), sampleY - row);
    };
    const phaseAt = (progress, sourceRank = .5, targetRank = .5) => {
        const fraction = clamp(progress);
        const dissolve = clamp((fraction - .15) / .29);
        const assemble = clamp((fraction - .6) / .34);
        const birth = .15 + sourceRank * .29;
        const arrival = .6 + targetRank * .34;
        const takeoff = smooth(birth, birth + .13, fraction);
        const landing = smooth(arrival - .15, arrival, fraction);
        const opacity = fraction <= .15 || fraction >= .94 ? 0
            : smooth(birth - .006, birth + .009, fraction) * (1 - smooth(arrival - .012, arrival + .006, fraction));
        return { dissolve, assemble, birth, arrival, takeoff, landing, opacity };
    };
    const layoutFor = (width, height) => {
        const compact = width <= 1100;
        const sourceWidth = width * (compact ? .98 : .58);
        const sourceHeight = sourceWidth * 853 / 1280;
        const targetHeight = Math.min(height * (width <= 600 ? .43 : compact ? .46 : .78), width * (compact ? 1.05 : .45) / .8);
        const targetWidth = targetHeight * .8;
        return { width, height, compact,
            source: [width * (compact ? .5 : .7) - sourceWidth / 2, height * (compact ? .72 : .51) - sourceHeight / 2, sourceWidth, sourceHeight],
            target: [width * (compact ? .5 : .27) - targetWidth / 2, height * (compact ? .73 : .53) - targetHeight / 2, targetWidth, targetHeight] };
    };
    const createField = (sourceSamples, targetSamples, count, sourceRanks, targetRanks) => {
        if (!sourceSamples.length || !targetSamples.length) throw new Error('Missing opaque image samples');
        const random = randomGenerator();
        const vertices = new Float32Array(count * 8);
        const timings = new Float32Array(count * 2);
        for (let index = 0; index < count; index++) {
            const source = sourceSamples[Math.floor(random() * sourceSamples.length)];
            const target = targetSamples[Math.floor(random() * targetSamples.length)];
            vertices.set([source[0], source[1], target[0], target[1], random() * Math.PI * 2,
                .18 + Math.sqrt(random()) * .82, .7 + random() * 2.1, random()], index * 8);
            timings.set([rankAt(sourceRanks, source[0], source[1]), rankAt(targetRanks, target[0], target[1])], index * 2);
        }
        return { vertices, timings, count };
    };
    const pointAt = (field, index, progress, layout) => {
        const offset = index * 8;
        const values = field.vertices;
        const phase = phaseAt(progress, field.timings[index * 2], field.timings[index * 2 + 1]);
        const angle = values[offset + 4] + progress * 1.7;
        const radius = values[offset + 5];
        const cloudX = layout.width * (.5 + Math.cos(angle) * radius * .46);
        const cloudY = layout.height * (.53 + Math.sin(angle * 1.3 + progress * 1.7) * radius * .3);
        const sourceX = layout.source[0] + values[offset] * layout.source[2];
        const sourceY = layout.source[1] + values[offset + 1] * layout.source[3];
        const targetX = layout.target[0] + values[offset + 2] * layout.target[2];
        const targetY = layout.target[1] + values[offset + 3] * layout.target[3];
        const depth = Math.sin(values[offset + 7] * 19.3) * 270 * phase.takeoff * (1 - phase.landing);
        const perspective = 1000 / (1000 - depth);
        return { x: (mix(mix(sourceX, cloudX, phase.takeoff), targetX, phase.landing) - layout.width / 2) * perspective + layout.width / 2,
            y: (mix(mix(sourceY, cloudY, phase.takeoff), targetY, phase.landing) - layout.height / 2) * perspective + layout.height / 2,
            depth, opacity: phase.opacity };
    };
    const samplesFromImage = (image) => {
        const canvas = document.createElement('canvas');
        canvas.width = 320;
        canvas.height = Math.round(320 * image.naturalHeight / image.naturalWidth);
        const context = canvas.getContext('2d', { willReadFrequently: true });
        context.drawImage(image, 0, 0, canvas.width, canvas.height);
        const pixels = context.getImageData(0, 0, canvas.width, canvas.height).data;
        const samples = [];
        for (let row = 0; row < canvas.height; row++) {
            for (let column = 0; column < canvas.width; column++) {
                const offset = (row * canvas.width + column) * 4;
                if (pixels[offset + 3] < 195) continue;
                const luminance = (pixels[offset] + pixels[offset + 1] + pixels[offset + 2]) / 765;
                samples.push([(column + .5) / canvas.width, (row + .5) / canvas.height]);
                if (luminance > .38) samples.push([(column + .5) / canvas.width, (row + .5) / canvas.height]);
            }
        }
        return samples;
    };
    const planeVertex = `
        attribute vec2 aPosition;
        uniform vec2 uResolution;
        uniform vec4 uRect;
        varying vec2 vUV;
        void main() {
            vUV = aPosition;
            vec2 pixel = uRect.xy + aPosition * uRect.zw;
            gl_Position = vec4(pixel.x / uResolution.x * 2.0 - 1.0, 1.0 - pixel.y / uResolution.y * 2.0, 0.0, 1.0);
        }`;
    const planeFragment = `
        precision highp float;
        uniform sampler2D uImage;
        uniform sampler2D uRanks;
        uniform float uCut;
        uniform float uTarget;
        varying vec2 vUV;
        void main() {
            vec4 colour = texture2D(uImage, vUV);
            if (colour.a < 0.003) discard;
            float rank = texture2D(uRanks, vUV).r;
            float keep = smoothstep(uCut - 0.012, uCut + 0.012, rank);
            if (uTarget > 0.5) keep = 1.0 - keep;
            if (uCut <= 0.0) keep = 1.0 - uTarget;
            if (uCut >= 1.0) keep = uTarget;
            if (keep < 0.003) discard;
            float edge = (1.0 - smoothstep(0.0, 0.025, abs(rank - uCut))) * step(0.001, uCut) * step(uCut, 0.999);
            gl_FragColor = vec4(colour.rgb + vec3(0.09, 0.105, 0.11) * edge, colour.a * keep);
        }`;
    const pointVertex = `
        precision highp float;
        attribute vec2 aSource;
        attribute vec2 aTarget;
        attribute vec4 aSeed;
        uniform vec2 uResolution;
        uniform vec4 uSourceRect;
        uniform vec4 uTargetRect;
        uniform sampler2D uSourceRank;
        uniform sampler2D uTargetRank;
        uniform float uProgress;
        uniform float uDpr;
        varying vec2 vSource;
        varying vec2 vTarget;
        varying float vOpacity;
        varying float vMaterial;
        varying float vAngle;
        void main() {
            float sourceRank = texture2D(uSourceRank, aSource).r;
            float targetRank = texture2D(uTargetRank, aTarget).r;
            float birth = 0.15 + sourceRank * 0.29;
            float arrival = 0.6 + targetRank * 0.34;
            float takeoff = smoothstep(birth, birth + 0.13, uProgress);
            float landing = smoothstep(arrival - 0.15, arrival, uProgress);
            float angle = aSeed.x + uProgress * 1.7;
            vec2 cloud = uResolution * vec2(0.5 + cos(angle) * aSeed.y * 0.46, 0.53 + sin(angle * 1.3 + uProgress * 1.7) * aSeed.y * 0.3);
            vec2 source = uSourceRect.xy + aSource * uSourceRect.zw;
            vec2 target = uTargetRect.xy + aTarget * uTargetRect.zw;
            float depth = sin(aSeed.w * 19.3) * 270.0 * takeoff * (1.0 - landing);
            float perspective = 1000.0 / (1000.0 - depth);
            vec2 pixel = (mix(mix(source, cloud, takeoff), target, landing) - uResolution * 0.5) * perspective + uResolution * 0.5;
            vOpacity = smoothstep(birth - 0.006, birth + 0.009, uProgress) * (1.0 - smoothstep(arrival - 0.012, arrival + 0.006, uProgress));
            vSource = aSource;
            vTarget = aTarget;
            vMaterial = smoothstep(0.48, 0.85, uProgress);
            vAngle = aSeed.x + takeoff * 3.0;
            gl_PointSize = clamp(aSeed.z * uDpr * perspective * (1.0 + takeoff * (1.0 - landing) * 0.45), 1.0, 9.0);
            gl_Position = vec4(pixel.x / uResolution.x * 2.0 - 1.0, 1.0 - pixel.y / uResolution.y * 2.0, 0.0, 1.0);
        }`;
    const pointFragment = `
        precision highp float;
        uniform sampler2D uSourceImage;
        uniform sampler2D uTargetImage;
        varying vec2 vSource;
        varying vec2 vTarget;
        varying float vOpacity;
        varying float vMaterial;
        varying float vAngle;
        void main() {
            vec2 offset = gl_PointCoord - 0.5;
            vec2 shard = mat2(cos(vAngle), -sin(vAngle), sin(vAngle), cos(vAngle)) * offset;
            float silhouette = abs(shard.x) * 0.72 + abs(shard.y) * 1.15;
            float alpha = (1.0 - smoothstep(0.3, 0.5, silhouette)) * vOpacity;
            if (alpha < 0.008) discard;
            vec3 source = texture2D(uSourceImage, vSource + shard * 0.004).rgb;
            vec3 target = texture2D(uTargetImage, vTarget + shard * 0.004).rgb;
            vec3 material = mix(source, target, vMaterial);
            float glint = pow(max(0.0, 1.0 - length(offset * vec2(1.2, 1.0)) * 2.0), 4.0) * 0.22;
            gl_FragColor = vec4(material * 1.55 + vec3(0.05, 0.06, 0.065) + glint, alpha);
        }`;
    const createRenderer = (canvas, images, count) => {
        let gl;
        try {
            gl = canvas.getContext('webgl', { alpha: true, antialias: false, depth: false,
                premultipliedAlpha: true, preserveDrawingBuffer: false, powerPreference: 'low-power' });
        } catch { return null; }
        if (!gl || gl.getParameter(gl.MAX_VERTEX_TEXTURE_IMAGE_UNITS) < 2) return null;
        const resources = { buffers: [], textures: [], programs: [] };
        const destroy = () => {
            resources.buffers.forEach((resource) => gl.deleteBuffer(resource));
            resources.textures.forEach((resource) => gl.deleteTexture(resource));
            resources.programs.forEach((resource) => gl.deleteProgram(resource));
        };
        try {
            const compile = (type, source) => {
                const shader = gl.createShader(type);
                gl.shaderSource(shader, source);
                gl.compileShader(shader);
                if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
                    const message = gl.getShaderInfoLog(shader);
                    gl.deleteShader(shader);
                    throw new Error(message);
                }
                return shader;
            };
            const program = (vertex, fragment, names) => {
                const shaderProgram = gl.createProgram();
                const vertexShader = compile(gl.VERTEX_SHADER, vertex);
                const fragmentShader = compile(gl.FRAGMENT_SHADER, fragment);
                gl.attachShader(shaderProgram, vertexShader);
                gl.attachShader(shaderProgram, fragmentShader);
                gl.linkProgram(shaderProgram);
                gl.deleteShader(vertexShader);
                gl.deleteShader(fragmentShader);
                resources.programs.push(shaderProgram);
                if (!gl.getProgramParameter(shaderProgram, gl.LINK_STATUS)) throw new Error(gl.getProgramInfoLog(shaderProgram));
                return { program: shaderProgram, locations: Object.fromEntries(names.map((name) => [name,
                    name.startsWith('a') ? gl.getAttribLocation(shaderProgram, name) : gl.getUniformLocation(shaderProgram, name)])) };
            };
            const surface = program(planeVertex, planeFragment, ['aPosition', 'uResolution', 'uRect', 'uImage', 'uRanks', 'uCut', 'uTarget']);
            const particles = program(pointVertex, pointFragment, ['aSource', 'aTarget', 'aSeed', 'uResolution', 'uSourceRect', 'uTargetRect',
                'uSourceRank', 'uTargetRank', 'uProgress', 'uDpr', 'uSourceImage', 'uTargetImage']);
            const sourceRanks = createRankMap(256);
            const targetRanks = createRankMap(256, true);
            const field = createField(samplesFromImage(images[0]), samplesFromImage(images[1]), count, sourceRanks, targetRanks);
            const buffer = (values) => {
                const resource = gl.createBuffer();
                resources.buffers.push(resource);
                gl.bindBuffer(gl.ARRAY_BUFFER, resource);
                gl.bufferData(gl.ARRAY_BUFFER, values, gl.STATIC_DRAW);
                return resource;
            };
            const planeBuffer = buffer(new Float32Array([0, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 1]));
            const particleBuffer = buffer(field.vertices);
            const texture = (input, map = false) => {
                const resource = gl.createTexture();
                resources.textures.push(resource);
                gl.bindTexture(gl.TEXTURE_2D, resource);
                gl.pixelStorei(gl.UNPACK_PREMULTIPLY_ALPHA_WEBGL, false);
                if (map) gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, input.size, input.size, 0, gl.RGBA, gl.UNSIGNED_BYTE, input.pixels);
                else gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, input);
                gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
                gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
                gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
                gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
                return resource;
            };
            const textures = [texture(images[0]), texture(images[1]), texture(sourceRanks, true), texture(targetRanks, true)];
            const bindTexture = (resource, unit, location) => {
                gl.activeTexture(gl.TEXTURE0 + unit);
                gl.bindTexture(gl.TEXTURE_2D, resource);
                gl.uniform1i(location, unit);
            };
            const attribute = (location, dimensions, stride = 0, offset = 0) => {
                gl.enableVertexAttribArray(location);
                gl.vertexAttribPointer(location, dimensions, gl.FLOAT, false, stride, offset);
            };
            let layout;
            let pixelRatio;
            let drawCount = count;
            const resize = (width, height, ratio) => {
                layout = layoutFor(width, height);
                pixelRatio = ratio;
                drawCount = Math.min(count, width > 1100 ? 12000 : width > 600 ? 7500 : 3200);
                const pixelWidth = Math.round(width * ratio);
                const pixelHeight = Math.round(height * ratio);
                if (canvas.width !== pixelWidth || canvas.height !== pixelHeight) {
                    canvas.width = pixelWidth;
                    canvas.height = pixelHeight;
                }
                gl.viewport(0, 0, canvas.width, canvas.height);
            };
            const replaceImages = (nextImages) => {
                gl.activeTexture(gl.TEXTURE0);
                nextImages.forEach((image, index) => {
                    gl.bindTexture(gl.TEXTURE_2D, textures[index]);
                    gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, image);
                });
            };
            const drawSurface = (target, cut) => {
                gl.useProgram(surface.program);
                gl.bindBuffer(gl.ARRAY_BUFFER, planeBuffer);
                attribute(surface.locations.aPosition, 2);
                gl.uniform2f(surface.locations.uResolution, layout.width, layout.height);
                gl.uniform4fv(surface.locations.uRect, target ? layout.target : layout.source);
                gl.uniform1f(surface.locations.uCut, cut);
                gl.uniform1f(surface.locations.uTarget, target ? 1 : 0);
                bindTexture(textures[target ? 1 : 0], 0, surface.locations.uImage);
                bindTexture(textures[target ? 3 : 2], 1, surface.locations.uRanks);
                gl.drawArrays(gl.TRIANGLES, 0, 6);
                gl.disableVertexAttribArray(surface.locations.aPosition);
            };
            const render = (progress) => {
                if (!layout || gl.isContextLost()) return null;
                const phase = phaseAt(progress);
                gl.clearColor(0, 0, 0, 0);
                gl.clear(gl.COLOR_BUFFER_BIT);
                gl.enable(gl.BLEND);
                gl.blendFuncSeparate(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA, gl.ONE, gl.ONE_MINUS_SRC_ALPHA);
                if (phase.dissolve < 1) drawSurface(false, phase.dissolve);
                if (phase.assemble > 0) drawSurface(true, phase.assemble);
                let detached = 0;
                if (drawCount > 0 && progress > .15 && progress < .94) {
                    gl.useProgram(particles.program);
                    gl.bindBuffer(gl.ARRAY_BUFFER, particleBuffer);
                    attribute(particles.locations.aSource, 2, 32, 0);
                    attribute(particles.locations.aTarget, 2, 32, 8);
                    attribute(particles.locations.aSeed, 4, 32, 16);
                    gl.uniform2f(particles.locations.uResolution, layout.width, layout.height);
                    gl.uniform4fv(particles.locations.uSourceRect, layout.source);
                    gl.uniform4fv(particles.locations.uTargetRect, layout.target);
                    gl.uniform1f(particles.locations.uProgress, progress);
                    gl.uniform1f(particles.locations.uDpr, pixelRatio);
                    bindTexture(textures[0], 0, particles.locations.uSourceImage);
                    bindTexture(textures[1], 1, particles.locations.uTargetImage);
                    bindTexture(textures[2], 2, particles.locations.uSourceRank);
                    bindTexture(textures[3], 3, particles.locations.uTargetRank);
                    gl.drawArrays(gl.POINTS, 0, drawCount);
                    ['aSource', 'aTarget', 'aSeed'].forEach((name) => gl.disableVertexAttribArray(particles.locations[name]));
                    for (let index = 0; index < drawCount; index++) {
                        const birth = .15 + field.timings[index * 2] * .29;
                        const arrival = .6 + field.timings[index * 2 + 1] * .34;
                        if (progress >= birth && progress < arrival) detached++;
                    }
                }
                return { detached, dissolve: phase.dissolve, assemble: phase.assemble,
                    phase: progress <= .15 ? 'solid-barbell' : progress < .44 ? 'disintegrating'
                        : progress < .6 ? 'cloud' : progress < .94 ? 'reconstructing' : 'solid-kettlebell' };
            };
            return { resize, replaceImages, render, destroy, get count() { return drawCount; } };
        } catch (error) {
            canvas.dataset.renderFailure = String(error.message);
            destroy();
            return null;
        }
    };

    return { clamp, mix, smooth, createRankMap, rankAt, phaseAt, layoutFor, createField, pointAt, createRenderer };
})();

if (typeof module !== 'undefined' && module.exports) module.exports = UTEFitSurface;
if (typeof window !== 'undefined') window.UTEFitSurface = UTEFitSurface;
