import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "/api/auth": {
        target: "http://localhost:8081",
        changeOrigin: true,
      },
      "/api/admin": {
        target: "http://localhost:8081",
        changeOrigin: true,
      },
      "/api/pengajuan-cuti": {
        target: "http://localhost:8081",
        changeOrigin: true,
      },
      "/api/persetujuan-cuti": {
        target: "http://localhost:8082",
        changeOrigin: true,
      },
    },
  },
});
