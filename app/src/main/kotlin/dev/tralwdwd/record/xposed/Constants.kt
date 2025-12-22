package dev.tralwdwd.record.xposed

class Constants {
    companion object {
        const val TARGET_PACKAGE = "com.discord"
        const val TARGET_ACTIVITY = "$TARGET_PACKAGE.react_activities.ReactActivity"

        const val FILES_DIR = "files/record"
        const val CACHE_DIR = "cache/record"
        const val MAIN_SCRIPT_FILE = "bundle.js"


        const val LOG_TAG = "ReCord"

        const val LOADER_NAME = "ReCordXposed"

        const val USER_AGENT = "ReCordXposed"
    }
}