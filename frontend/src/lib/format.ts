const numberFormat = new Intl.NumberFormat();
const decimalFormat = new Intl.NumberFormat(undefined, {maximumFractionDigits: 1});
const progressFormat = new Intl.NumberFormat(undefined, {minimumFractionDigits: 1, maximumFractionDigits: 1});

export function formatNumber(value: number): string {
    return numberFormat.format(value);
}

export function formatProgress(progress: number): string {
    return `${progressFormat.format(progress)}%`;
}

export function formatBytes(bytes: number): string {
    if (bytes < 1024) {
        return `${numberFormat.format(bytes)} B`;
    }
    const divisor = bytes < 1024 * 1024 ? 1024 : 1024 * 1024;
    return `${decimalFormat.format(bytes / divisor)} ${divisor === 1024 ? "KiB" : "MiB"}`;
}
