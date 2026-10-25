(() => {
    const start = document.getElementById('qr-camera-start');
    if (!start) return;
    const stop = document.getElementById('qr-camera-stop');
    const video = document.getElementById('qr-camera');
    const status = document.getElementById('qr-camera-status');
    const imageInput = document.getElementById('qr-image');
    const form = document.getElementById('qr-checkin-form');
    const canvas = document.createElement('canvas');
    const context = canvas.getContext('2d', {willReadFrequently: true});
    const tokenPattern = /^UTEFITQ[RO]\.[A-Za-z0-9_-]{43}$/;
    let stream = null;
    let timer = null;
    let generation = 0;

    const closeCamera = () => {
        generation += 1;
        clearTimeout(timer);
        timer = null;
        const activeStream = stream;
        stream = null;
        activeStream?.getTracks().forEach(track => track.stop());
        video.pause();
        video.srcObject = null;
        video.hidden = true;
        stop.hidden = true;
        start.disabled = false;
    };

    const readPixels = (source, width, height, limit = 960) => {
        if (!context || !width || !height || typeof window.jsQR !== 'function') return null;
        const scale = Math.min(1, limit / Math.max(width, height));
        canvas.width = Math.max(1, Math.round(width * scale));
        canvas.height = Math.max(1, Math.round(height * scale));
        context.drawImage(source, 0, 0, canvas.width, canvas.height);
        const pixels = context.getImageData(0, 0, canvas.width, canvas.height);
        return window.jsQR(pixels.data, pixels.width, pixels.height, {inversionAttempts: 'attemptBoth'})?.data ?? null;
    };

    const acceptToken = value => {
        if (!tokenPattern.test(value ?? '')) return false;
        form.elements.qrToken.value = value;
        closeCamera();
        status.textContent = 'Đã đọc QR và dừng camera. Kiểm tra chi nhánh rồi nhấn Xác nhận QR; backend xác định Check-in hay Check-out. Nếu mã hết hạn, Member cần làm mới.';
        return true;
    };

    const cameraError = error => {
        if (error.name === 'NotAllowedError' || error.name === 'SecurityError') {
            return 'Camera bị từ chối. Cho phép Camera trong cài đặt website của Safari/Chrome rồi thử lại. Bạn vẫn có thể chọn ảnh QR hoặc dán mã.';
        }
        if (error.name === 'NotFoundError' || error.name === 'OverconstrainedError') {
            return 'Không tìm thấy camera phù hợp. Chọn ảnh QR hoặc dán mã để tiếp tục.';
        }
        if (error.name === 'NotReadableError' || error.name === 'AbortError') {
            return 'Camera đang bận hoặc không khởi động được. Đóng ứng dụng đang dùng camera rồi thử lại, hoặc chọn ảnh QR.';
        }
        return 'Không đọc được camera. Kiểm tra quyền camera và HTTPS, thử lại hoặc dùng ảnh QR / nhập mã tại quầy.';
    };

    const nativeDetector = async () => {
        if (!window.BarcodeDetector) return null;
        try {
            if (typeof window.BarcodeDetector.getSupportedFormats === 'function') {
                const formats = await window.BarcodeDetector.getSupportedFormats();
                if (!formats.includes('qr_code')) return null;
            }
            return new window.BarcodeDetector({formats: ['qr_code']});
        } catch (error) {
            return null;
        }
    };

    stop.addEventListener('click', () => {
        closeCamera();
        status.textContent = 'Đã dừng camera. Có thể mở lại, chọn ảnh QR hoặc dán mã.';
    });
    window.addEventListener('pagehide', closeCamera);
    form.addEventListener('submit', closeCamera);
    form.elements.branchId.addEventListener('change', () => {
        closeCamera();
        form.elements.qrToken.value = '';
        status.textContent = 'Đã đổi chi nhánh. Quét lại QR hoặc dán mã để tránh xác nhận nhầm.';
    });
    document.addEventListener('visibilitychange', () => {
        if (document.hidden) {
            closeCamera();
            status.textContent = 'Đã dừng camera khi rời màn hình. Nhấn Mở camera để quét tiếp.';
        }
    });

    start.addEventListener('click', async () => {
        if (!form.elements.branchId.value) {
            status.textContent = 'Chọn chi nhánh trước khi quét.';
            return;
        }
        if (!window.isSecureContext || !navigator.mediaDevices?.getUserMedia) {
            status.textContent = 'Camera cần website HTTPS và trình duyệt hỗ trợ getUserMedia. Mở URL HTTPS Cloudflare trong Safari/Chrome, hoặc chọn ảnh QR / dán mã.';
            return;
        }
        closeCamera();
        const session = generation;
        start.disabled = true;
        stop.hidden = false;
        status.textContent = 'Đang mở camera. Hãy cho phép truy cập camera khi trình duyệt hỏi.';
        try {
            let detector = await nativeDetector();
            if (session !== generation) return;
            if (!detector && (typeof window.jsQR !== 'function' || !context)) {
                throw new Error('QR decoder unavailable');
            }
            const acquired = await navigator.mediaDevices.getUserMedia({
                video: {facingMode: {ideal: 'environment'}, width: {ideal: 1280}, height: {ideal: 720}},
                audio: false
            });
            if (session !== generation) {
                acquired.getTracks().forEach(track => track.stop());
                return;
            }
            stream = acquired;
            stream.getVideoTracks().forEach(track => track.addEventListener('ended', () => {
                if (session !== generation) return;
                closeCamera();
                status.textContent = 'Camera đã ngắt. Mở lại camera hoặc chọn ảnh QR / dán mã.';
            }, {once: true}));
            video.srcObject = stream;
            video.hidden = false;
            video.muted = true;
            await video.play();
            if (session !== generation) return;
            status.textContent = 'Đưa toàn bộ QR Check-in / Check-out vào camera, giữ máy ổn định. Không dùng QR chuyển khoản.';
            const scan = async () => {
                if (session !== generation || !stream) return;
                try {
                    if (video.readyState >= 2 && video.videoWidth && video.videoHeight) {
                        let value = null;
                        if (detector) {
                            try {
                                const results = await detector.detect(video);
                                value = results.find(result => tokenPattern.test(result.rawValue))?.rawValue ?? null;
                            } catch (error) {
                                detector = null;
                                if (typeof window.jsQR !== 'function' || !context) throw error;
                            }
                        }
                        if (session !== generation) return;
                        if (!detector) value = readPixels(video, video.videoWidth, video.videoHeight);
                        if (acceptToken(value)) return;
                    }
                    if (session === generation) timer = setTimeout(scan, 300);
                } catch (error) {
                    if (session !== generation) return;
                    closeCamera();
                    status.textContent = cameraError(error);
                }
            };
            await scan();
        } catch (error) {
            if (session !== generation) return;
            closeCamera();
            status.textContent = cameraError(error);
        }
    });

    imageInput.addEventListener('change', () => {
        closeCamera();
        const session = generation;
        const file = imageInput.files?.[0];
        imageInput.value = '';
        if (!file) return;
        if (!form.elements.branchId.value) {
            status.textContent = 'Chọn chi nhánh trước khi đọc ảnh QR.';
            return;
        }
        if (!file.type.startsWith('image/') || file.size > 10 * 1024 * 1024) {
            status.textContent = 'Chọn file ảnh hợp lệ tối đa 10 MB; hoặc dán mã QR.';
            return;
        }
        if (typeof window.jsQR !== 'function' || !context) {
            status.textContent = 'Không tải được bộ đọc ảnh QR cục bộ. Tải lại trang hoặc dán mã.';
            return;
        }
        status.textContent = 'Đang đọc ảnh QR trên thiết bị...';
        const image = new Image();
        const url = URL.createObjectURL(file);
        image.onload = () => {
            URL.revokeObjectURL(url);
            if (session !== generation) return;
            try {
                const value = readPixels(image, image.naturalWidth, image.naturalHeight, 1280);
                if (!acceptToken(value)) status.textContent = 'Không thấy QR Check-in / Check-out hợp lệ trong ảnh. Chọn ảnh rõ hơn hoặc dán mã.';
            } catch (error) {
                status.textContent = 'Không đọc được ảnh. Thử ảnh PNG/JPEG rõ hơn hoặc dán mã.';
            }
        };
        image.onerror = () => {
            URL.revokeObjectURL(url);
            if (session === generation) status.textContent = 'Trình duyệt không mở được ảnh này. Dùng PNG/JPEG hoặc dán mã.';
        };
        image.src = url;
    });
})();
