import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Emits a minimal server bundle for the runtime Docker stage.
  output: "standalone",
  reactStrictMode: true,
  poweredByHeader: false,
  typescript: {
    // Never ship a build that does not typecheck.
    ignoreBuildErrors: false,
  },
  eslint: {
    ignoreDuringBuilds: false,
  },
};

export default nextConfig;
