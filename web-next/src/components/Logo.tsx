// The Pulse mark: a square tile with a heartbeat line. Fixed colours, so it looks the same in both themes.
export function Logo({ size = 32 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" aria-label="Pulse" role="img">
      <rect width="32" height="32" rx="3" fill="#0f9d8a" />
      <polyline points="5,17 11,17 14,9 18,23 21,17 27,17" fill="none" stroke="#fff"
                strokeWidth="2.6" strokeLinejoin="miter" strokeLinecap="square" />
    </svg>
  );
}
