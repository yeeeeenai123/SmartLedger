# SmartLedger ProGuard Rules
-keepattributes *Annotation*
-keep class com.smartledger.app.data.database.** { *; }
-dontwarn javax.annotation.**
