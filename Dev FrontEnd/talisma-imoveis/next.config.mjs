/** @type {import('next').NextConfig} */
const config = {
  experimental: { serverActions: { bodySizeLimit: "52mb" } },
};

export default config;
