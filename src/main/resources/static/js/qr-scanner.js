(() => {
    const start = document.getElementById('qr-camera-start');
    if (!start) return;
    const stop = document.getElementById('qr-camera-stop');
    const video = document.getElementById('qr-camera');
    const status = document.getElementById('qr-camera-status');
    const form = document.getElementById('qr-checkin-form');
    let stream;
    let timer;
    const closeCamera = () => {
        clearTimeout(timer);
        stream?.getTracks().forEach(track => track.stop());
        stream = null;
        video.srcObject = null;
        video.hidden = true;
        stop.hidden = true;
        start.disabled = false;
    };
    stop.addEventListener('click', closeCamera);
    window.addEventListener('pagehide', closeCamera);
    start.addEventListener('click', async () => {
        if (!form.branchId.value) {
            status.textContent = 'Chọn chi nhánh trước khi quét.';
            return;
        }
        if (!window.BarcodeDetector || !navigator.mediaDevices?.getUserMedia) {
            status.textContent = 'Trình duyệt chưa hỗ trợ quét QR. Dùng thiết bị hỗ trợ hoặc nhập mã tại quầy.';
            return;
        }
        try {
            const detector = new BarcodeDetector({formats: ['qr_code']});
            stream = await navigator.mediaDevices.getUserMedia({video: {facingMode: 'environment'}, audio: false});
            video.srcObject = stream;
            video.hidden = false;
            stop.hidden = false;
            start.disabled = true;
            await video.play();
            status.textContent = 'Đưa QR vào trước camera.';
            const scan = async () => {
                if (!stream) return;
                try {
                    const results = await detector.detect(video);
                    const found = results.find(result => /^UTEFITQR\.[A-Za-z0-9_-]{43}$/.test(result.rawValue));
                    if (found) {
                        form.qrToken.value = found.rawValue;
                        closeCamera();
                        status.textContent = 'Đã quét. Kiểm tra chi nhánh và nhấn Xác nhận QR.';
                        return;
                    }
                    timer = setTimeout(scan, 400);
                } catch (error) {
                    closeCamera();
                    status.textContent = 'Không đọc được QR. Thử lại hoặc nhập mã tại quầy.';
                }
            };
            await scan();
        } catch (error) {
            closeCamera();
            status.textContent = 'Không mở được camera. Kiểm tra quyền camera và HTTPS, hoặc nhập mã tại quầy.';
        }
    });
})();
