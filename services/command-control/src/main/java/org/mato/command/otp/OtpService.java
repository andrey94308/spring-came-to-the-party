package org.mato.command.otp;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Deterministic 4-digit OTP service. Like a stripped-down TOTP: HMAC-SHA256
 * over a 30-second window, take the low 4 digits.
 *
 * No rate limiting on /verify-otp, so the 10 000-code search space can be
 * attempted within the 30-second window by parallelising requests.
 */
@Service
public class OtpService {

    private final byte[] seed;
    private final long windowSeconds;
    private final Clock clock;

    public OtpService(@Value("${auth.otp-seed}") String seed,
                      @Value("${auth.otp-window-seconds}") long windowSeconds,
                      Clock clock) {
        this.seed = seed.getBytes(StandardCharsets.UTF_8);
        this.windowSeconds = windowSeconds;
        this.clock = clock;
    }

    public String currentCode() {
        long window = clock.instant().getEpochSecond() / windowSeconds;
        return code(window);
    }

    public boolean verify(String otp) {
        if (otp == null || otp.length() != 4) return false;
        return currentCode().equals(otp);
    }

    private String code(long window) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(seed, "HmacSHA256"));
            byte[] digest = mac.doFinal(ByteBuffer.allocate(8).putLong(window).array());
            int n = ((digest[0] & 0x7F) << 24)
                  | ((digest[1] & 0xFF) << 16)
                  | ((digest[2] & 0xFF) << 8)
                  | (digest[3] & 0xFF);
            return String.format("%04d", n % 10000);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
