import type { NextConfig } from "next";

// REST calls go to /api on this same origin and Next forwards them to Spring Boot,
// so the browser never needs CORS for them. WebSockets are not proxied by rewrites,
// so the STOMP client connects to the backend directly (see NEXT_PUBLIC_WS_URL).
const backendUrl = process.env.BACKEND_URL ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  cacheComponents: true,
  partialPrefetching: true,
  async rewrites() {
    return [{ source: "/api/:path*", destination: `${backendUrl}/api/:path*` }];
  },
};

export default nextConfig;
