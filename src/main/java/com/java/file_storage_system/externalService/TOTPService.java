package com.java.file_storage_system.externalService;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TOTPService {

    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    // 1. Khi user muốn BẬT 2FA
    public String setup2FA(String username) {
        // Tạo Secret Key chuẩn
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        String secretKey = key.getKey();

        // TODO: LƯU `secretKey` vào Database gắn với `username`

        // Tạo URL để hiển thị QR Code cho user quét
        String issuer = "MyAwesomeSystem";
        String otpAuthURL = GoogleAuthenticatorQRGenerator.getOtpAuthURL(issuer, username, key);

        System.out.println("Secret Key lưu DB: " + secretKey);
        System.out.println("URL để tạo QR Code: " + otpAuthURL);

        return otpAuthURL; // Trả URL này về Frontend để thư viện tạo QR Code (ví dụ: qrcode.js)
    }

    // 2. Khi user ĐĂNG NHẬP và nhập mã 6 số
    public boolean verifyLogin(String username, int userEnteredCode) {
        // TODO: Query Database tìm `secretKey` của `username`
        String secretKeyFromDB = "LẤY_TỪ_DATABASE_RA";

        // Hàm authorize tự động xử lý Time Drift và validation
        boolean isValid = gAuth.authorize(secretKeyFromDB, userEnteredCode);

        if (isValid) {
            System.out.println("Đăng nhập thành công!");
            return true;
        } else {
            System.out.println("Mã không hợp lệ hoặc đã hết hạn.");
            return false;
        }
    }
}