package com.vibe.stashdrop.core.shizuku;
import com.vibe.stashdrop.core.shizuku.IClipboardCallback;
interface IPrivilegedService {
    String setAssistantRole(String packageName, int userId) = 0;
    String readClipboard() = 1;
    void startClipboard(IClipboardCallback callback) = 2;
    void stopClipboard() = 3;
    String getAssistantRole(int userId) = 5;
    String grantSetup(int userId) = 6;
    String routeAssistantToStashDrop(String googleVoiceService, int userId) = 7;
    boolean requestGoogleSearch() = 8;
    void destroy() = 16777114;
}
