const COLORS = ["#0f9d8a", "#d6455b", "#5b5bd6", "#d68a2f", "#2f86d6", "#9b4fd6"];

function colorFor(name: string) {
  let hash = 0;
  for (const char of name) hash = (hash * 31 + char.charCodeAt(0)) | 0;
  return COLORS[Math.abs(hash) % COLORS.length];
}

interface AvatarProps {
  name: string;
  size?: number;
  online?: boolean;
}

/** Initials in a coloured circle, with an optional online dot. */
export function Avatar({ name, size = 32, online }: AvatarProps) {
  const initials = name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]!.toUpperCase())
    .join("");

  return (
    <span
      style={{
        position: "relative", display: "inline-flex", alignItems: "center", justifyContent: "center",
        width: size, height: size, borderRadius: 3, background: colorFor(name), color: "#fff",
        fontWeight: 700, fontSize: size * 0.4, flexShrink: 0,
      }}
      aria-hidden
    >
      {initials}
      {online !== undefined && (
        <span
          style={{
            position: "absolute", right: -1, bottom: -1, width: 9, height: 9, borderRadius: 3,
            background: online ? "#2f9e5b" : "#9aa1ad", border: "2px solid var(--surface)",
          }}
        />
      )}
    </span>
  );
}
