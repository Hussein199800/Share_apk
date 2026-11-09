package com.example.installer;

// Runs inside Shizuku's process (shell uid). Returns "<exit code>\n<stdout+stderr>".
interface IShellService {
    String run(in String[] cmd, in ParcelFileDescriptor stdin) = 1;

    // Required by Shizuku to tear the service down.
    void destroy() = 16777114;
}
