package io.github.spaceleam.cadence.core;

/**
 * Interface utama untuk Rate Limiter.
 * Semua implementasi rate limiting strategy harus implement interface ini.
 */
public interface RateLimiter {

    /**
     * Coba ambil 1 token dari bucket.
     * 
     * @return true kalau berhasil dapat token, false kalau bucket empty
     */
    boolean tryAcquire();

    /**
     * Coba ambil sejumlah token dari bucket (weighted request).
     * 
     * @param tokens jumlah token yang mau diambil
     * @return true kalau berhasil, false kalau token tidak cukup
     */
    boolean tryAcquire(int tokens);

    /**
     * Cek berapa token yang tersedia saat ini.
     * 
     * @return jumlah token yang available
     */
    int getAvailableTokens();

    /**
     * Reset bucket ke full capacity.
     */
    void reset();
}
