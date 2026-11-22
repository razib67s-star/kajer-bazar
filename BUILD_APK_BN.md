# কাজের বাজার APK — ইনস্টল ও পরীক্ষার নির্দেশনা

## পরীক্ষামূলক APK কোথায় পাবেন
1. GitHub-এ [Kajer Bazar Actions](https://github.com/razib67s-star/kajer-bazar/actions) খুলুন।
2. সর্বশেষ **Build Kajer Bazar APK** রান নির্বাচন করুন।
3. রান সফল হলে **Artifacts** থেকে `Kajer-Bazar-Android-APK` ডাউনলোড করুন।
4. ZIP Extract করে `Kajer-Bazar-Android.apk` ফাইলটি ইনস্টল করুন।

## ফোনে ইনস্টল না হলে
- Android-এর Auto Blocker বা Play Protect বন্ধ করবেন না।
- একই অ্যাপের আগের পরীক্ষামূলক সংস্করণ ইনস্টল থাকলে, সেটি আপডেট করার সময় signing-key mismatch হতে পারে। পুরোনো অ্যাপের ভেতরে প্রয়োজনীয় তথ্য থাকলে আগে সংরক্ষণ করুন; তারপরই কেবল পুরোনো পরীক্ষামূলক অ্যাপ uninstall করে নতুন APK ইনস্টল পরীক্ষা করুন। Uninstall করলে অ্যাপের স্থানীয় তথ্য মুছে যেতে পারে।
- সতর্কবার্তা এলে জোর করে ইনস্টল করবেন না। সঠিক উৎস ও APK যাচাই ছাড়া নিরাপত্তা উপেক্ষা করবেন না।

## এই বিল্ডের সীমা
এটি GitHub Actions-এর debug/test APK; Play Store বা জনসাধারণের জন্য স্বাক্ষরিত production release নয়। Build ও APK signature যাচাই সফল হওয়া মানেই Samsung ফোনে ইনস্টল/চালু হওয়া নিশ্চিত নয়। ফোনে বাস্তব ইনস্টল পরীক্ষা এখনও বাকি।

## এখনো প্রয়োজনীয় কাজ
- Supabase Phone Auth ও SMS Provider কনফিগারেশন
- অ্যাডমিন অ্যাকাউন্ট bootstrap ও বাস্তব OTP পরীক্ষা
- প্রোফাইল সম্পাদনা, নথি যাচাই, নোটিফিকেশন ও সম্পূর্ণ কাজ-সমাপ্তি প্রবাহের UI পরীক্ষা
- লাইভ পেমেন্টের জন্য অনুমোদিত merchant account ও নিরাপদ backend integration
- production signing key, privacy/legal review এবং বাস্তব ডিভাইসে QA

## নিরাপত্তা
- Play Protect বা Android security feature বন্ধ করবেন না।
- Supabase service-role key, SMS secret বা payment secret Android অ্যাপ/GitHub-এ রাখবেন না।
- কর্মী agreed wage-এর পুরো টাকা পাবেন; নিয়োগদাতা তার অতিরিক্ত ১০% কমিশন দেবেন। লাইভ টাকা সংগ্রহ এখনো চালু নয়।
