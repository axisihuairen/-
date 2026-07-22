#include <windows.h>
#include <stdio.h>
#include <stdlib.h>

static int findJava(char* outPath, int outSize) {
    FILE* fp = _popen("where javaw 2>nul", "r");
    if (fp) {
        if (fgets(outPath, outSize, fp)) {
            outPath[strcspn(outPath, "\r\n")] = '\0';
            _pclose(fp);
            return 1;
        }
        _pclose(fp);
    }
    HKEY hKey;
    if (RegOpenKeyExA(HKEY_LOCAL_MACHINE, "SOFTWARE\\JavaSoft\\Java Runtime Environment", 0, KEY_READ | KEY_WOW64_64KEY, &hKey) == ERROR_SUCCESS) {
        char version[32];
        DWORD size = sizeof(version);
        if (RegQueryValueExA(hKey, "CurrentVersion", NULL, NULL, (LPBYTE)version, &size) == ERROR_SUCCESS) {
            char subKey[128];
            snprintf(subKey, sizeof(subKey), "SOFTWARE\\JavaSoft\\Java Runtime Environment\\%s", version);
            HKEY hKey2;
            if (RegOpenKeyExA(HKEY_LOCAL_MACHINE, subKey, 0, KEY_READ | KEY_WOW64_64KEY, &hKey2) == ERROR_SUCCESS) {
                char path[MAX_PATH];
                DWORD pathSize = sizeof(path);
                if (RegQueryValueExA(hKey2, "JavaHome", NULL, NULL, (LPBYTE)path, &pathSize) == ERROR_SUCCESS) {
                    snprintf(outPath, outSize, "%s\\bin\\javaw.exe", path);
                    RegCloseKey(hKey2);
                    RegCloseKey(hKey);
                    return 1;
                }
                RegCloseKey(hKey2);
            }
        }
        RegCloseKey(hKey);
    }
    return 0;
}

int WINAPI WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine, int nCmdShow) {
    char javaPath[MAX_PATH];
    char exePath[MAX_PATH];
    char jarPath[MAX_PATH];
    char cmdLine[1024];

    if (!GetModuleFileNameA(NULL, exePath, MAX_PATH)) {
        MessageBoxA(NULL, "无法获取程序路径", "错误", MB_OK | MB_ICONERROR);
        return 1;
    }

    char* lastSlash = strrchr(exePath, '\\');
    if (!lastSlash) lastSlash = strrchr(exePath, '/');
    if (lastSlash) *(lastSlash + 1) = '\0';

    snprintf(jarPath, sizeof(jarPath), "%sFloatingEra.jar", exePath);

    if (GetFileAttributesA(jarPath) == INVALID_FILE_ATTRIBUTES) {
        MessageBoxA(NULL, "未找到 FloatingEra.jar，请确保它与本程序在同一目录。", "错误", MB_OK | MB_ICONERROR);
        return 1;
    }

    if (!findJava(javaPath, sizeof(javaPath))) {
        MessageBoxA(NULL, "未找到 Java 运行时环境，请先安装 Java。\n推荐: https://adoptium.net/", "错误", MB_OK | MB_ICONERROR);
        return 1;
    }

    snprintf(cmdLine, sizeof(cmdLine), "\"%s\" -jar \"%s\"", javaPath, jarPath);

    STARTUPINFOA si = { sizeof(si) };
    PROCESS_INFORMATION pi = { 0 };

    if (!CreateProcessA(NULL, cmdLine, NULL, NULL, FALSE, 0, NULL, exePath, &si, &pi)) {
        MessageBoxA(NULL, "启动游戏失败", "错误", MB_OK | MB_ICONERROR);
        return 1;
    }

    CloseHandle(pi.hProcess);
    CloseHandle(pi.hThread);
    return 0;
}
