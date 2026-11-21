# কাজের বাজার APK তৈরির নির্দেশনা

## GitHub থেকে পরীক্ষামূলক APK
1. GitHub-এ `razib67s-star/kajer-bazar` repository খুলুন।
2. **Actions** ট্যাবে যান।
3. **Build Kajer Bazar APK** workflow নির্বাচন করুন।
4. **Run workflow** চাপুন (যদি workflow-টি manual run সমর্থন করে)।
5. Build সম্পন্ন হলে workflow run খুলে **Artifacts** থেকে `Kajer-Bazar-Android-APK` ডাউনলোড করুন।
6. ZIP Extract করে `Kajer-Bazar-Android.apk` ফাইলটি পাবেন।

## গুরুত্বপূর্ণ নিরাপত্তা
- এই APK debug/test build; production release নয়।
- Android/Play Protect সতর্কতা এলে নিরাপত্তা বন্ধ করবেন না বা জোর করে Install করবেন না। আগে build/source যাচাই করুন।
- Production release-এর জন্য আলাদা release signing key নিরাপদে সংরক্ষণ করতে হবে; key বা password GitHub repository-তে আপলোড করা যাবে না।
- bKash/Nagad payment secret কখনো Android APK-তে রাখা যাবে না। অনুমোদিত merchant ও secure backend লাগবে।

## বর্তমান অ্যাপের সীমাবদ্ধতা
বর্তমান অ্যাপটি প্রাথমিক demo। বাস্তব OTP login, server/database, job posting/application, admin dashboard, payment ও commission collection এখনো সম্পন্ন নয়।

## পণ্যের সিদ্ধান্ত ও উন্নয়ন
বাংলাদেশজুড়ে কাজদাতা ও কর্মীদের marketplace এবং সফল কাজপ্রতি ১০% কমিশনের প্রাথমিক নীতি [ROADMAP_BN.md](ROADMAP_BN.md)-এ নথিভুক্ত। কমিশন কে দেবে, সফল কাজের সংজ্ঞা, বাতিল/ফেরতের নিয়ম ও payment provider চূড়ান্ত না হওয়া পর্যন্ত live commission collection চালু করা যাবে না।
