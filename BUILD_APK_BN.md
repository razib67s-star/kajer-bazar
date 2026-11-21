# কাজের বাজার APK তৈরির নির্দেশনা

## GitHub থেকে APK তৈরি
1. এই ZIP ফাইলটি Extract করুন।
2. GitHub-এ `razib67s-star/kajer-bazar` repository খুলুন।
3. **Add file → Upload files** থেকে এই ফোল্ডারের ভেতরের সব ফাইল ও `.github` ফোল্ডার আপলোড করে Commit changes দিন। (পুরোনো ফাইলের সঙ্গে একই ফাইল থাকলে নতুনগুলো দিয়ে Replace করুন।)
4. Repository-এর **Actions** ট্যাবে যান।
5. **Build Kajer Bazar APK** workflow নির্বাচন করে **Run workflow** চাপুন।
6. Build সফল হলে workflow run-এর নিচে **Artifacts → kajer-bazar-debug-apk** থেকে APK ডাউনলোড করুন।
7. ফোনে APK খুলে Install দিন। Android চাইলে ওই উৎস থেকে Install অনুমতি দিন।

## গুরুত্বপূর্ণ
এই ZIP-এ থাকা অ্যাপটি প্রাথমিক ডেমো: বাংলা হোম স্ক্রিন ও নমুনা কাজের কার্ড আছে। বাস্তব লগইন, কাজ পোস্ট/ম্যানেজমেন্ট, সার্ভার/ডাটাবেস, bKash/Nagad পেমেন্ট ও কমিশন এখনো তৈরি হয়নি। এই ZIP থেকে APK এখনো তৈরি বা ডিভাইসে পরীক্ষা করা হয়নি।
