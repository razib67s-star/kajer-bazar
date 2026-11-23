# কাজের বাজার — Android App

বাংলাদেশজুড়ে কর্মী ও কাজদাতাদের সংযুক্ত করার বাংলা কাজের মার্কেটপ্লেস।

## ব্যবসায়িক নিয়ম
- বাজার: বাংলাদেশ, জেলা/উপজেলা ভিত্তিক
- ব্যবহারকারী: কর্মী ও কাজদাতা
- কমিশন: কর্মীর agreed wage-এর অতিরিক্ত ১০% কাজদাতা দেবেন
- উদাহরণ: কর্মীর মজুরি ৳১,০০০ হলে কাজদাতা মোট ৳১,১০০ দেবেন; কর্মী পুরো ৳১,০০০ পাবেন
- ভাষা: বাংলা

## বর্তমান বাস্তবায়ন
- Supabase schema, RLS এবং secure job lifecycle RPCs প্রয়োগ করা হয়েছে।
- Android-এ email/password signup-login এবং optional Bangladesh phone OTP request/verify flow যোগ করা হয়েছে।
- Public job feed, authenticated profile, worker application, employer verification request, moderated job posting এবং admin review UI যুক্ত আছে।
- Database API grants ও profile-role escalation protection migration প্রয়োগ করা হয়েছে।
- Payment foundation এবং sandbox order function আছে; live payment নেই।
- GitHub Actions-এ unit test, APK signature/package check এবং Android emulator install/launch check সফল হয়েছে (combined-auth build).
- OTP/SMS provider, admin dashboard UI, scheduler, notification, document verification, session persistence এবং production release এখনও অসম্পূর্ণ।

## Build
GitHub Actions-এর **Build Kajer Bazar APK** workflow debug APK তৈরি ও emulator test করে। সফল run-এর artifact-ই ব্যবহার করুন। Debug APK production release নয়।

## এখনো অসম্পূর্ণ
- Supabase SMS provider configuration ও বাস্তব OTP end-to-end test।
- Encrypted session persistence/refresh, employer applicant-management UI, full admin dashboard এবং overdue-completion scheduler।
- Release signing, real-device test, privacy/terms ও production readiness review।

## Security
- Play Protect বা Android security feature বন্ধ করে install করবেন না।
- Publishable key ছাড়া কোনো secret Android app-এ রাখবেন না।
- SMS/payment secrets শুধু provider/Supabase secret manager-এ থাকবে।
- Production চালুর আগে security, legal, privacy, moderation ও payment review আবশ্যক।

বিস্তারিত: [BACKEND_SETUP_BN.md](BACKEND_SETUP_BN.md), [PAYMENTS_SETUP_BN.md](PAYMENTS_SETUP_BN.md), [ROADMAP_BN.md](ROADMAP_BN.md)।
