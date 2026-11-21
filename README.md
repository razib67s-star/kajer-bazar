# কাজের বাজার — Android App

বাংলাদেশজুড়ে কাজদাতা ও কর্মীদের সংযুক্ত করার বাংলা চাকরি/কাজের মার্কেটপ্লেস।

## অনুমোদিত পণ্যের সিদ্ধান্ত
- ব্যবহারকারী: কাজদাতা ও কর্মী
- এলাকা: বাংলাদেশ (জেলা/উপজেলা ভিত্তিক)
- কমিশন: সফল কাজের মোট সম্মত মূল্যের ১০% (প্রাথমিক নীতি; কমিশন কে বহন করবে এবং বিরোধ/ফেরতের নিয়ম চূড়ান্ত করতে হবে)
- ভাষা: বাংলা

## উন্নয়ন পরিকল্পনা
সম্পূর্ণ বৈশিষ্ট্য, নিরাপত্তা, পেমেন্ট, কমিশন এবং ধাপভিত্তিক বাস্তবায়নের জন্য [ROADMAP_BN.md](ROADMAP_BN.md) দেখুন।

## বর্তমান অবস্থা
বর্তমান Android অ্যাপটি প্রাথমিক demo। বাস্তব OTP login, backend/database, কাজ প্রকাশ ও আবেদন, admin dashboard, payment gateway এবং commission collection এখনো চালু নয়। Roadmap-এ লেখা থাকা মানে feature তৈরি বা সক্রিয় হয়েছে—এমন নয়।

## APK build
GitHub Actions-এর **Build Kajer Bazar APK** workflow চালিয়ে build artifact সংগ্রহ করা যায়। বর্তমান debug APK পরীক্ষার জন্য; এটি production release নয়।

## নিরাপত্তা
- Play Protect বা Android নিরাপত্তা ব্যবস্থা বন্ধ করে ইনস্টল করবেন না।
- Production signing key, payment secret বা OTP secret কখনো repository-তে commit করবেন না।
- বাস্তব পেমেন্ট কেবল অনুমোদিত merchant account, provider approval এবং secure backend দিয়ে চালু হবে।
