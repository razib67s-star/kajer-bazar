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

## নতুন ফিচার
- কর্মীরা প্রকাশিত কাজ নিজের তালিকায় সংরক্ষণ করতে পারবেন; সংরক্ষিত কাজ শুধু নিজের অ্যাকাউন্টেই দেখা যাবে।
- কাজ খোঁজার সময় নাম/বর্ণনা, জেলা, উপজেলা ও কাজের বিভাগ দিয়ে ফিল্টার করা যায়।
- সংরক্ষিত কাজের তালিকা অ্যাকাউন্টের সঙ্গে সার্ভারে থাকবে, তাই অন্য ডিভাইসে একই অ্যাকাউন্টে লগইন করলেও পাওয়া যাবে।
- সংরক্ষিত কাজের ফিচারের Android build ও emulator পরীক্ষা চলমান।

## Build
GitHub Actions-এর **Build Kajer Bazar APK** workflow debug APK তৈরি ও emulator test করে। সফল run-এর artifact-ই ব্যবহার করুন। Debug APK production release নয়।

## এখনো অসম্পূর্ণ
- Supabase SMS provider configuration ও বাস্তব OTP end-to-end test।
- Android Keystore-এ refresh token encryption, app start-এ session refresh এবং logout-এ server sign-out/local clear code যোগ হয়েছে; emulator instrumentation verification pending।
- Encrypted session persistence/refresh-এর real-device edge-case test, full admin dashboard এবং overdue-completion scheduler।
- Release signing, real-device test, privacy/terms ও production readiness review।

## ক্যামেরা ও ভিডিও ফিচার
- অ্যাপের নিচের **ভিডিও** ট্যাবে সরাসরি ক্যামেরা দিয়ে ছবি তোলা, গ্যালারি থেকে ছবি/ভিডিও নির্বাচন এবং কমিউনিটি ফিডে পোস্ট করা যায়।
- ছবি/ভিডিও Supabase Storage-এর `community-media` bucket-এ যায়; পোস্ট টেবিল ও Storage-এ RLS দিয়ে ব্যবহারকারীভিত্তিক আপলোড নিয়ন্ত্রণ আছে।
- ভিডিও ফিডে প্রকাশিত ছবি ও ভিডিও দেখা যায়। সর্বোচ্চ ফাইল আকার ৫০ MB; ভিডিও ফরম্যাট MP4, MOV ও 3GP।
- লাইভ ভিডিও কল আপাতত Jitsi Meet-এর আলাদা রুম ব্রাউজারে খোলে; এটি অ্যাপের ভিতরে বিল্ট-ইন WebRTC নয় এবং অপর পক্ষকে একই রুম লিংক পাঠাতে হয়।
- নতুন media screen-এর Android build ও বাস্তব ডিভাইস পরীক্ষা CI-তে যাচাই করতে হবে।

## Restart from workflow run #238
- Baseline is commit `3224e66207e1ee2d44b089c37645c95e4284a98c`, from workflow run #238.
- Follow-up work is isolated on branch `feature/from-run-238`; do not rewrite the original branch.
- First priority: reproduce and diagnose the reported email/password login behavior that unexpectedly opens Downloads. Source review so far did not find an explicit Downloads intent in the login handler; the exact trigger still needs device-side reproduction.
- Add the requested social feed, profile, jobs/services, marketplace, camera/video, messaging, notifications and safety improvements in tested stages.
- No claim of a fixed login issue or new APK until a focused test confirms it.

## Security
- Play Protect বা Android security feature বন্ধ করে install করবেন না।
- Publishable key ছাড়া কোনো secret Android app-এ রাখবেন না।
- SMS/payment secrets শুধু provider/Supabase secret manager-এ থাকবে।
- Production চালুর আগে security, legal, privacy, moderation ও payment review আবশ্যক।

বিস্তারিত: [BACKEND_SETUP_BN.md](BACKEND_SETUP_BN.md), [PAYMENTS_SETUP_BN.md](PAYMENTS_SETUP_BN.md), [ROADMAP_BN.md](ROADMAP_BN.md)।
