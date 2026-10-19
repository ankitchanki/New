package com.vibe.stashdrop.core.shizuku;
import android.os.ParcelFileDescriptor;
import com.vibe.stashdrop.core.shizuku.IClipboardCallback;
interface IPrivilegedService {
    String setAssistantRole(String packageName, int userId) = 0;
    String readClipboard() = 1;
    void startClipboard(IClipboardCallback callback) = 2;
    void stopClipboard() = 3;
    ParcelFileDescriptor captureToPipe() = 4;
    String getAssistantRole(int userId) = 5;
    String grantSetup(int userId) = 6;
    void destroy() = 16777114;
}
