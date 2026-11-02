package com.ctu.cinema;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class TicketBookingSystem {

    private static final int TOTAL_SEATS = 50;

    // Mảng trạng thái ghế (Index 1..50). Ban đầu mặc định false (Chưa đặt)
    private final boolean[] seats = new boolean[TOTAL_SEATS + 1];

    private final ReentrantLock lock = new ReentrantLock();

    private final AtomicInteger successfulBookings = new AtomicInteger(0);
    private final AtomicInteger failedBookings = new AtomicInteger(0);

    /**
     * Phương thức đặt vé cho ghế seatNumber
     * CHÚ Ý: Phiên bản ban đầu CHƯA ĐỒNG BỘ - Dễ bị nổ lỗi Race Condition!
     *
     * @param seatNumber   Số ghế cần đặt (1..50)
     * @param customerName Tên khách hàng đặt vé
     * @return true nếu đặt thành công, false nếu ghế đã bị chiếm
     */

    public boolean bookSeat(int seatNumber, String customerName) {
        if (seatNumber < 1 || seatNumber > TOTAL_SEATS) {
            System.err.println("❌ Ghế không hợp lệ: " + seatNumber);
            return false;
        }
        lock.lock();

        // --- MIỀN TỐI HẠN (CRITICAL SECTION) CHƯA ĐƯỢC BẢO VỆ ---
        try {
            if (!seats[seatNumber]) {
                try {Thread.sleep(10);} catch (InterruptedException e) {}
                seats[seatNumber] = true;
                successfulBookings.incrementAndGet();
                System.out.println("✅ [Thành công] " + customerName + " đã đặt thành công ghế #" + seatNumber);
                return true;
            } else {
                failedBookings.incrementAndGet();
                System.out.println("❌ [Thất bại] " + customerName + " đặt ghế #" + seatNumber + " nhưng ghế ĐÃ BỊ CHIẾM!");
                return false;
            }
        } finally {
            lock.unlock(); // Luôn giải phóng khóa trong block finally!
        }
        // --------------------------------------------------------
    }

    public int getSuccessfulBookings() {
        return successfulBookings.get();
    }

    public int getFailedBookings() {
        return failedBookings.get();
    }

    // Test Runner giả lập 100 khách hàng đặt vé đồng thời trên Thread Pool
    public static void main(String[] args) throws InterruptedException {
        TicketBookingSystem bookingSystem = new TicketBookingSystem();
        ExecutorService executor = Executors.newFixedThreadPool(10); // Pool 10 luồng chạy song song
        System.out.println("🚀 Bắt đầu mô phỏng 100 yêu cầu đặt vé đồng thời...");
        for (int i = 1; i <= 100; i++) {
            final int customerId = i;
            // 100 khách hàng tranh nhau đặt ngẫu nhiên các ghế từ 1 đến 50
            final int targetSeat = (i % 50) + 1;
            executor.submit(() -> {
                bookingSystem.bookSeat(targetSeat, "Khách hàng #" + customerId);
            });

        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        System.out.println("\n========= THỐNG KÊ TỔNG KẾT (UNSAFE RUN) =========");
        System.out.println("Tổng số lượt báo THÀNH CÔNG: " + bookingSystem.getSuccessfulBookings());
        System.out.println("Tổng số lượt báo THẤT BẠI: " + bookingSystem.getFailedBookings());
        System.out.println("Tổng số yêu cầu đã xử lý: " + (bookingSystem.getSuccessfulBookings() + bookingSystem.getFailedBookings()));
        System.out.println("=================================================");
    }
}