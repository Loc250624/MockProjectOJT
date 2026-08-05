/*
 * Chỉ dùng khi form Teacher upload/chọn video nhưng backend chưa nhận duration.
 * Codex phải tích hợp vào JS hiện có, không thêm file production riêng nếu không cần.
 */
export function bindVideoDurationCapture(videoElement, hiddenDurationInput) {
    if (!videoElement || !hiddenDurationInput) {
        return;
    }

    videoElement.addEventListener("loadedmetadata", () => {
        const duration = Number(videoElement.duration);
        if (Number.isFinite(duration) && duration > 0) {
            hiddenDurationInput.value = String(Math.ceil(duration));
        }
    });
}
