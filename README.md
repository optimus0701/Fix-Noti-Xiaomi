# 🚀 Fix-Noti-Xiaomi

**Fix-Noti-Xiaomi** là ứng dụng Android chuyên dụng giúp khắc phục triệt để tình trạng **chậm / trễ thông báo (Notification Delay)** trên các dòng thiết bị Xiaomi, Redmi và POCO (đặc biệt đối với các bản ROM nội địa Trung Quốc - China ROM, MIUI và HyperOS).

Ứng dụng sử dụng **[Shizuku](https://shizuku.rikka.app/)** để tương tác trực tiếp với các dịch vụ hệ thống Android cấp thấp, giúp tối ưu hóa ứng dụng **KHÔNG CẦN ROOT** thiết bị hay mở khóa Bootloader.

---

## ✨ Tính năng nổi bật

- ⚡ **Sửa trễ thông báo 1-Click**: Tối ưu hóa hàng loạt nhiều ứng dụng cùng lúc (Zalo, Facebook Messenger, Telegram, WhatsApp, Instagram, các ứng dụng Ngân hàng & Ví điện tử...).
- 🛡️ **Tối ưu hóa đa tầng qua Shizuku**:
  - **DeviceIdle Whitelist**: Đưa ứng dụng vào danh sách bỏ qua cơ chế tiết kiệm pin sâu (Doze Mode).
  - **Standby Bucket**: Đặt trạng thái ứng dụng về `ACTIVE` (mức ưu tiên tài nguyên cao nhất của Android).
  - **AppOps Background**: Bật quyền chạy ngầm `RUN_IN_BACKGROUND` và `RUN_ANY_IN_BACKGROUND`.
  - **MIUI/HyperOS System Whitelists**: Thêm ứng dụng vào các bảng danh sách trắng của hệ thống Xiaomi (`millet_white`, `cloud_lowlatency_whitelist`, `MILLET_NO_RESTRICT_APP`).
- 🔍 **Quản lý & Kiểm tra chi tiết**: Hiển thị trạng thái của 8 chỉ số tối ưu hóa cho từng ứng dụng, hỗ trợ bật/tắt hoặc khôi phục quyền thủ công.
- 🌐 **Cập nhật danh sách ứng dụng đề xuất qua CDN**: Tải tự động danh sách các ứng dụng phổ biến từ CDN tốc độ cao với cơ chế fallback và bộ nhớ đệm thông minh.
- 🎨 **Giao diện hiện đại**: Xây dựng bằng Jetpack Compose với Material Design 3, trải nghiệm mượt mà, hỗ trợ giao diện tối (Dark Mode).

---

## 🛠️ Nguyên lý hoạt động

Trễ thông báo trên ROM Xiaomi China chủ yếu do 3 nguyên nhân:
1. **Doze Mode & Standby Bucket**: Hệ thống hạ cấp ứng dụng xuống nhóm hạn chế (`RARE` / `RESTRICTED`) khiến ứng dụng không nhận được tin nhắn FCM/GCM tức thì.
2. **Hạn chế AppOps**: Xiaomi chặn quyền chạy ngầm của các ứng dụng bên thứ 3.
3. **Tính năng Millet (Xiaomi Power Management)**: Bộ đóng băng ứng dụng nền của MIUI/HyperOS tự động "đóng băng" ứng dụng sau vài phút tắt màn hình.

**Fix-Noti-Xiaomi** giải quyết triệt để các rào cản trên bằng cách gửi lệnh quản trị trực tiếp thông qua Shizuku Service để đưa ứng dụng vào danh sách ưu tiên hàng đầu của hệ thống.

---

## 📋 Yêu cầu hệ thống

- Thiết bị: Xiaomi / Redmi / POCO chạy MIUI 12+ hoặc HyperOS (Android 7.0 trở lên).
- Yêu cầu tiên quyết: Ứng dụng **[Shizuku](https://shizuku.rikka.app/)** đã được cài đặt và đang ở trạng thái **Đang chạy (Running)**.

---

## 📱 Hướng dẫn sử dụng

1. Tải và cài đặt ứng dụng **Shizuku** từ Google Play hoặc GitHub.
2. Kích hoạt Shizuku (qua ADB không dây trên điện thoại hoặc kết nối với máy tính).
3. Mở **Fix-Noti-Xiaomi** và cấp quyền Shizuku khi được hỏi.
4. Tích chọn các ứng dụng bạn muốn khắc phục trễ thông báo và bấm **"Tối ưu hóa ứng dụng đã chọn"**.

---

## 📄 Giấy phép (License)

Dự án này được phát hành dưới giấy phép [MIT License](LICENSE).
