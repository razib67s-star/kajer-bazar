# কাজের বাজার — পূর্ণাঙ্গ উন্নয়ন পরিকল্পনা

## অনুমোদিত ব্যবসায়িক সিদ্ধান্ত
- বাজার: বাংলাদেশজুড়ে (জেলা ও উপজেলা ভিত্তিক)
- ব্যবহারকারী: কাজদাতা এবং কর্মী—উভয় পক্ষ
- আয়ের মডেল: সফল কাজপ্রতি কমিশন
- কমিশনের হার: কাজের মোট সম্মত মূল্যের ১০%
- কমিশন দেবেন: কাজদাতা; কর্মীর সম্মত পারিশ্রমিক থেকে কমিশন কাটা হবে না
- প্রধান ভাষা: বাংলা
- Android অ্যাপ: Kotlin ও Jetpack Compose

## কমিশন নীতি (অনুমোদিত মূল নিয়ম; অপারেশনাল শর্ত বাকি)
১. কাজদাতা সফলভাবে কাজ সম্পন্ন হলে কাজের সম্মত মূল্যের অতিরিক্ত ১০% কমিশন দেবেন।
২. উদাহরণ: কর্মীর পারিশ্রমিক ১,০০০ টাকা হলে কাজদাতা মোট ১,১০০ টাকা দেবেন; কর্মী পাবেন ১,০০০ টাকা এবং কাজের বাজারের কমিশন ১০০ টাকা।
৩. কাজের বিজ্ঞাপন/চুক্তির আগে কর্মী ও কাজদাতা উভয়কে পারিশ্রমিক, অতিরিক্ত কমিশন এবং মোট প্রদেয় অর্থ স্পষ্টভাবে দেখাতে হবে।
৪. কাজ শেষ হলে কর্মী ও কাজদাতা—দুজনকেই আলাদাভাবে কাজ সম্পন্ন হয়েছে বলে নিশ্চিত করতে হবে। কর্মী কাজ শেষের অনুরোধ পাঠালে কাজদাতাকে ৩ ক্যালেন্ডার দিনের মধ্যে নিশ্চিত বা আপত্তি জানাতে হবে; সময়সীমার আগে অ্যাপ স্মরণবার্তা পাঠাবে।
৫. ৩ দিনের মধ্যে কাজদাতা সাড়া না দিলে কাজটি স্বয়ংক্রিয়ভাবে Admin Review কিউতে যাবে। নীরবতাকে সম্মতি ধরা হবে না; প্রশাসনিক পর্যালোচনা ও সিদ্ধান্ত ছাড়া কাজ Completed হবে না এবং কমিশনও চূড়ান্ত/আদায় হবে না।
৬. কোনো পক্ষ আপত্তি জানালে কাজটি Dispute Review-তে যাবে; উভয় পক্ষকে প্রমাণ/ব্যাখ্যা দেওয়ার সুযোগ দিয়ে অ্যাডমিন সিদ্ধান্ত নেবেন।
৭. বাতিল/ফেরত নীতি এবং কমিশন আদায়ের পদ্ধতি নির্দিষ্ট করতে হবে।
৮. অনুমোদিত মার্চেন্ট/পেমেন্ট গেটওয়ে ও নিরাপদ সার্ভার ছাড়া বাস্তব bKash/Nagad লেনদেন চালু করা যাবে না। APK-তে কোনো গোপন API key বা merchant secret রাখা যাবে না।

## পণ্য বৈশিষ্ট্য
### কর্মী
- ফোন OTP নিবন্ধন/লগইন, প্রোফাইল ও পরিচয় যাচাইয়ের অবস্থা
- জেলা/উপজেলা, কাজের বিভাগ, দক্ষতা ও পারিশ্রমিক অনুযায়ী কাজ খোঁজা
- কাজের বিস্তারিত দেখা, আবেদন, আবেদন প্রত্যাহার
- নির্বাচিত/চলমান/সম্পন্ন কাজের অবস্থা
- কাজদাতার সঙ্গে নিরাপদ যোগাযোগ
- কাজ শেষে রেটিং, পর্যালোচনা ও অভিযোগ

### কাজদাতা
- কাজ পোস্ট, সম্পাদনা, বিরতি ও বন্ধ করা
- কাজের বিবরণ, অবস্থান, সময়, পারিশ্রমিক ও শর্ত উল্লেখ
- আবেদনকারী দেখা, নির্বাচন/প্রত্যাখ্যান
- কাজ শুরু ও সম্পন্ন নিশ্চিত করা
- কর্মীকে রেটিং এবং বিরোধ/অভিযোগ দাখিল
- কাজের সম্মত পারিশ্রমিকের অতিরিক্ত ১০% কমিশন পরিশোধ

### অ্যাডমিন
- ব্যবহারকারী ও পরিচয় যাচাইয়ের কিউ
- বিজ্ঞাপন অনুমোদন/প্রত্যাখ্যান ও রিপোর্ট পর্যালোচনা
- কাজ, আবেদন, বিরোধ ও নিষিদ্ধ অ্যাকাউন্ট পরিচালনা
- পেমেন্ট, কমিশন, ফেরত ও হিসাবের অডিট লগ
- ভূমিকা-ভিত্তিক অ্যাক্সেস, অ্যাডমিন কার্যক্রমের রেকর্ড
- বিভাগ, জেলা/উপজেলা ও নীতিমালা পরিচালনা

## প্রস্তাবিত কাজের বিভাগ
বাসাবাড়ির কাজ; ডেলিভারি; দোকান ও বিক্রয়; নির্মাণ ও মেরামত; পরিচ্ছন্নতা; রান্না/খাবার; কৃষি; নিরাপত্তা; অফিস সহায়তা; অনলাইন/ডিজিটাল কাজ; অস্থায়ী ও দৈনিক কাজ।

## কাজের জীবনচক্র
Draft → Pending moderation → Published → Applications → Worker selected → In progress → Worker completion request → Employer response within 3 calendar days (reminder sent) → both confirm: Completed → Commission settlement; employer silent for 3 days: Admin Review (not automatic completion/payment); either party disputes: Dispute Review → admin decision.
বাতিল, অনুপস্থিতি, অভিযোগ, বিরোধ ও ফেরতের জন্য পৃথক অবস্থা এবং অডিট রেকর্ড রাখতে হবে।

## প্রযুক্তিগত স্থাপত্য
- Android: Kotlin, Jetpack Compose, MVVM, Repository pattern
- Backend: authenticated REST API (প্রযুক্তি নির্বাচন আলাদা নকশা পর্যায়ে)
- Database: PostgreSQL; migration ও automated backup
- OTP: নির্ভরযোগ্য SMS provider; rate limiting ও abuse prevention
- File storage: private object storage; signed upload/download
- Push: Firebase Cloud Messaging
- Admin: secure web dashboard with role-based access control
- Monitoring: crash/error logging, audit events, alerts
- CI: automated build, unit tests, dependency checks; release signing secrets GitHub Secrets-এ সীমিত ও সুরক্ষিতভাবে

## নিরাপত্তা ও গোপনীয়তা
- HTTPS/TLS বাধ্যতামূলক; server-side authorization
- OTP brute-force ও spam সীমিত করা
- ব্যক্তিগত তথ্য ন্যূনতম সংগ্রহ, উদ্দেশ্য/সম্মতি স্পষ্ট করা
- password/OTP/payment PIN কখনো log বা support chat-এ না রাখা
- sensitive data encryption, backup, retention ও deletion policy
- report/block, fraud detection, admin audit log
- প্রকাশের আগে threat model, security review ও penetration test
- বাংলাদেশে প্রযোজ্য আইন, ভোক্তা অধিকার, শ্রম/চাকরি সংক্রান্ত বাধ্যবাধকতা ও পেমেন্ট সেবাদাতার শর্ত আইনজীবী/বিশেষজ্ঞের মাধ্যমে যাচাই

## বাস্তবায়নের পর্যায় ও গ্রহণযোগ্যতার শর্ত
### Phase 0 — ভিত্তি ও প্রয়োজনীয়তা
- repository/build structure যাচাই ও পরিষ্কার
- product requirements, screen flow, data model, API contract
- দ্বিপক্ষীয় কাজ-সম্পন্ন নিশ্চিতকরণ, সফল কাজের সংজ্ঞা, বাতিল/ফেরত নীতি চূড়ান্ত
**Done:** অনুমোদিত requirements ও architecture নথি।

### Phase 1 — কার্যকর Android MVP (পেমেন্ট ছাড়া)
- navigation, জেলা/উপজেলা নির্বাচন, বিভাগ, job search/filter
- OTP auth integration প্রস্তুত; test environment
- worker/employer profiles, job posting, application, status workflow
- demo data বাদ দিয়ে API-ভিত্তিক data
**Done:** দুই test account দিয়ে end-to-end job flow server-এ সংরক্ষিত।

### Phase 2 — Backend ও Admin
- API, database, moderation, role-based admin, audit log
- notification, report/block, backups, monitoring
**Done:** unauthorized access tests pass; admin workflow verified.

### Phase 3 — কমিশন ও পেমেন্ট
- merchant onboarding ও gateway sandbox integration
- server-side commission calculation at 10%, payable by employer in addition to worker's agreed pay
- payment reconciliation, refunds, dispute handling
- receipts and transaction history
**Done:** sandbox tests pass; gateway/provider and legal approvals obtained before live transactions.

### Phase 4 — নিরাপত্তা ও বাংলাদেশব্যাপী প্রস্তুতি
- load, device compatibility, accessibility, security testing
- privacy policy, terms, commission/refund policy, support channel
- pilot rollout, monitoring and incident response
**Done:** critical/high issues resolved; pilot sign-off.

### Phase 5 — Release
- production release keystore securely managed (never commit to repo)
- signed release APK/AAB, Play Console internal testing
- staged rollout and rollback plan
**Done:** release verified and operational support ready.

## এখনো যেসব সিদ্ধান্ত/অনুমোদন বাকি
- কাজ বাতিল/অসম্পূর্ণ হলে কমিশন ও ফেরতের নিয়ম কী?
- কাজ বাতিল/অসম্পূর্ণ হলে কমিশন ও ফেরতের নিয়ম কী?
- সর্বনিম্ন/সর্বোচ্চ কাজের মূল্য বা কমিশন সীমা থাকবে কি?
- OTP/SMS provider, payment gateway ও merchant account নির্বাচন
- Terms, Privacy Policy, support contact ও dispute policy অনুমোদন

## বর্তমান বাস্তবায়নের অবস্থা — ২৪ নভেম্বর ২০২৬

### কোড ও ডেটাবেসে যা যুক্ত হয়েছে
- Supabase PostgreSQL schema, RLS, auth profile trigger এবং ন্যূনতম PostgREST grants প্রয়োগ করা হয়েছে।
- নতুন ব্যবহারকারীর default role worker; profile owner নিজে role পরিবর্তন করতে পারে না।
- Email/password signup-login এবং Bangladesh phone OTP request/verify code আছে; email login বাদ দেওয়া হয়নি।
- Android Keystore-এ AES-GCM encrypted refresh-token storage, app launch-এ session refresh এবং logout-এ server sign-out/local clear code যুক্ত।
- Public job feed, worker application, application history/withdrawal, employer verification request, moderated job posting, employer applicant review/worker selection এবং সীমিত admin moderation UI যুক্ত।
- Public job read-এ column-level grants ব্যবহার করা হয়েছে; private work_address anonymous বা authenticated client-কে দেওয়া হয়নি।
- GitHub Actions-এ unit tests, APK signature/package verification, Android emulator launch এবং encrypted session instrumentation tests চালানো হচ্ছে।

### এখনো production-ready নয়
- Supabase Dashboard-এ SMS provider credentials/configuration এবং বাস্তব বাংলাদেশি নম্বরে OTP delivery test বাকি। SMS provider ছাড়া OTP পাঠানো যাবে না।
- Connected Supabase project-এ বর্তমানে registered users, profiles ও jobs শূন্য; তাই পুরোনো email account ওই project-এ আছে কি না যাচাই করা যায়নি। Email login code রাখা হয়েছে; কোনো account মুছে ফেলা হয়নি।
- Profile edit, district/upazila search/filter, employer contact-sharing rules, full admin dashboard, notification, report/block, rating, dispute evidence and admin resolution বাকি।
- Overdue completion scheduler, real payment gateway, refund/settlement, merchant approval, privacy policy/terms এবং support workflow বাকি।
- Release keystore/AAB signing, real-device testing, end-to-end email/OTP/job workflow tests এবং production security review বাকি।

বর্তমান debug APK বা CI build-কে সম্পূর্ণ production app বলা যাবে না। প্রতিটি অসম্পূর্ণ ধাপ বাস্তব পরীক্ষা ও অনুমোদনসহ শেষ না হওয়া পর্যন্ত ব্যবহারকারীর কাছে final install file দেওয়া হবে না।


## ব্যবহারকারীর নতুন অনুমোদিত সিদ্ধান্ত — ২৫ নভেম্বর ২০২৬

- লক্ষ্য ব্যবহারকারী: সবাই। কর্মী ও কাজদাতা ছাড়াও কোম্পানি, দোকানদার এবং সাধারণ দর্শনার্থী অ্যাপ ব্যবহার করতে পারবেন। তবে সংবেদনশীল কাজ (আবেদন, পোস্ট, মেসেজ, প্রোফাইল পরিবর্তন) করতে লগইন বাধ্যতামূলক থাকবে।
- সাধারণ দর্শনার্থী প্রথমে প্রকাশিত কাজ ও অনুমোদিত পাবলিক কনটেন্ট দেখতে পারবেন; ব্যক্তিগত তথ্য, ব্যক্তিগত মেসেজ বা সীমাবদ্ধ যোগাযোগের তথ্য দেখতে পারবেন না।
- কাজের ধরন অনুযায়ী কর্মী/কাজদাতা প্রোফাইল বেছে নেওয়া যাবে। কোম্পানি/দোকানদারের জন্য কাজদাতা প্রোফাইল ও প্রতিষ্ঠান-তথ্য যাচাইয়ের ধাপ থাকবে।
- উন্নয়ন ও MVP পর্যায়ে বাজেট শূন্য: নতুন paid API, paid SDK, subscription বা বাধ্যতামূলক paid service যোগ করা যাবে না। বিদ্যমান বিনামূল্যের সীমার মধ্যে কাজ হবে; কোনো ফ্রি-টায়ার সীমা অতিক্রমের ঝুঁকি দেখা দিলে আগে জানাতে হবে।
- বাস্তব SMS OTP, পেমেন্ট গেটওয়ে, SMS/Push delivery বা লাইভ লেনদেন কেবল প্রয়োজনীয় provider/merchant configuration ও ব্যবহারকারীর অনুমোদনের পর চালু হবে। কোনো ডেমো/স্যান্ডবক্স পেমেন্টকে বাস্তব টাকা হিসেবে দেখানো যাবে না।
- সব ফিচার একসঙ্গে পরিকল্পিত থাকবে, কিন্তু বাস্তবায়ন ধাপে ধাপে হবে। প্রতিটি ধাপের আগে acceptance criteria, পরে automated test এবং যেখানে সম্ভব বাস্তব ডিভাইস পরীক্ষা লাগবে।

### সম্প্রসারিত বাস্তবায়ন তালিকা
1. **Core job marketplace:** চাকরি/কাজ পোস্ট, জেলা-উপজেলা ও বিভাগভিত্তিক সার্চ, ফিল্টার, আবেদন, আবেদন-অবস্থা এবং কর্মী/কাজদাতা প্রোফাইল।
2. **Trust & safety:** পরিচয়/প্রতিষ্ঠান যাচাইয়ের অবস্থা, রিপোর্ট, ব্লক, প্রতারণা সতর্কতা, নিরাপদ যোগাযোগ, গোপনীয়তা নিয়ন্ত্রণ এবং অ্যাডমিন অডিট লগ।
3. **Community & messaging:** পোস্ট/ছবি/ভিডিও, লাইক/কমেন্ট, ব্যক্তিগত মেসেজ, অনুপযুক্ত কনটেন্ট রিপোর্ট ও moderation।
4. **Retention & accessibility:** বাংলা-প্রথম UI, ইংরেজি ভাষা, কম ডেটা ব্যবহার, ছোট স্ক্রিনে responsive layout, notification preferences এবং সহজ onboarding।
5. **Operations:** পূর্ণাঙ্গ admin dashboard, অভিযোগ/বিরোধ ব্যবস্থাপনা, support workflow, monitoring ও backup restore test।
6. **Payments (পরে):** অনুমোদিত gateway sandbox, refund/dispute workflow ও reconciliation; live payment-এর আগে আইনগত, merchant এবং security approval।

### বর্তমান বিল্ড যাচাইয়ের নিয়ম
- GitHub Actions-এ unit test, debug APK assemble, signature/package verification এবং Android emulator install/launch সফল না হওয়া পর্যন্ত build-কে verified বলা যাবে না।
- CI-তে তৈরি debug/test APK-কে production release বলা যাবে না। ব্যবহারকারীর ফোনে install ও গুরুত্বপূর্ণ user journey পরীক্ষা আলাদা acceptance step।
- Supabase Security Advisor-এর SECURITY DEFINER সতর্কতা প্রতিটি function-এর body ও authorization check যাচাই করে triage করতে হবে; সতর্কতা আছে বলেই অন্ধভাবে function permission খুলে/বন্ধ করা যাবে না।


## ব্যবহারকারীর পছন্দের নতুন পেজ ও ফিচার — ২৫ নভেম্বর ২০২৬

### অ্যাপের মূল নেভিগেশন
1. **কাজের বাজার / হোম পেজ:** নতুন কাজ, গুরুত্বপূর্ণ ঘোষণা ও দ্রুত প্রবেশ।
2. **চাকরি ও নতুন পোস্ট:** প্রকাশিত কাজ দেখা, সার্চ ও ফিল্টার।
3. **চাকরি:** কাজের বিস্তারিত, আবেদন এবং আবেদনের অবস্থা।
4. **কর্মী:** কর্মী খোঁজা; নিয়োগকর্তার পছন্দের কর্মী সংরক্ষণ ও পরে তালিকা দেখা।
5. **মেসেজ:** ব্যক্তিগত যোগাযোগ; কেবল অনুমোদিত অ্যাকাউন্টের মধ্যে।
6. **ভিডিও ও পোস্ট:** কাজের নমুনা, প্রচার ও কমিউনিটি কনটেন্ট।
7. **আমার প্রোফাইল:** ব্যক্তিগত/প্রতিষ্ঠান তথ্য, দক্ষতা, সেটিংস ও কার্যক্রম।
8. **অভিযোগ ও সহায়তা:** সমস্যা/প্রতারণা রিপোর্ট, সহায়তার অনুরোধ ও উত্তর/অবস্থা দেখা।

### নতুন বাস্তবায়িত ডেটাবেস ভিত্তি
- `public.saved_workers`: নিয়োগকর্তা নিজের পছন্দের সক্রিয় কর্মী সংরক্ষণ/অপসারণ করতে পারবেন; RLS দিয়ে কেবল নিজের তালিকা দেখা যাবে এবং কর্মী-ভূমিকা যাচাই ছাড়া সংরক্ষণ করা যাবে না।
- `public.support_tickets`: লগইন করা ব্যবহারকারী অভিযোগ/সহায়তার অনুরোধ জমা দিতে ও নিজের অনুরোধের অবস্থা/অ্যাডমিন উত্তর দেখতে পারবেন; অ্যাডমিন-শুধু আপডেট নীতি প্রযোজ্য।
- এই টেবিল দুটিতে RLS চালু এবং সীমিত grants দেওয়া হয়েছে। Security Advisor-এ পূর্ববর্তী SECURITY DEFINER function-সংক্রান্ত সতর্কতা ও leaked-password protection সতর্কতা এখনও আলাদা করে পর্যালোচনার বিষয়।

### Android UI সংযোজন
- কর্মী ডিরেক্টরি ও পছন্দের কর্মী সংরক্ষণের স্ক্রিন যুক্ত।
- অভিযোগ ও সহায়তা জমা, নিজের অনুরোধের অবস্থা ও অ্যাডমিন উত্তরের স্ক্রিন যুক্ত।
- নেভিগেশনে কর্মী এবং অভিযোগ/সহায়তার প্রবেশপথ যুক্ত।
- নতুন UI-এর CI build, emulator এবং বাস্তব অ্যাকাউন্ট দিয়ে backend flow পরীক্ষা না হওয়া পর্যন্ত ফিচারগুলোকে সম্পূর্ণ/production-ready বলা যাবে না।

### ভবিষ্যৎ পেমেন্ট
- পেমেন্ট পেজ/অর্ডার/লেনদেনের অবস্থা ভবিষ্যতের ধাপ হিসেবে থাকবে।
- বর্তমান MVP-তে কোনো live payment বা আসল টাকা গ্রহণ চালু নয়। Merchant approval, provider configuration, আইনগত যাচাই, refund/dispute policy এবং নিরাপত্তা পরীক্ষা সম্পন্ন হওয়ার পরই sandbox; তার পর আলাদা অনুমোদনে live payment।


## স্বয়ংক্রিয় পর্যালোচনা — ২৭ নভেম্বর ২০২৬

### এই পর্যালোচনায় যাচাই করা হয়েছে
- Supabase project status: ACTIVE_HEALTHY; migration তালিকা ও Security/Performance/Health advisors পুনরায় পরীক্ষা করা হয়েছে।
- Performance: আগের ৭টি অনুপস্থিত foreign-key index সতর্কতা সমাধান হয়েছে; saved_workers-এর worker_id index এখন অনুপস্থিত হিসেবে দেখায়নি।
- Security: SECURITY DEFINER function সতর্কতা এবং leaked-password protection সতর্কতা follow-up হিসেবে রয়েছে; function authorization body পরীক্ষা ছাড়া permission পরিবর্তন করা হয়নি।
- Android physical-device, SMS OTP, Push delivery, live payment, release signing ও বাস্তব বহু-ব্যবহারকারীর flow পরীক্ষা CI/backend access থেকে নিশ্চিত করা যায় না; এগুলো blocker/acceptance step হিসেবে থাকবে।

### পরবর্তী নিরাপদ অগ্রাধিকার
1. Admin dashboard/account deletion flow-এর source ও test coverage পর্যালোচনা; account deletion-এর আগে data-retention ও recovery policy নিশ্চিত করা।
2. Notifications এবং overdue-completion scheduler-এর implementation/CI coverage পর্যালোচনা; বাস্তব delivery বা scheduled execution না হলে সেভাবে চিহ্নিত করা।
3. Media upload/feed, private messaging, saved workers, support tickets-এর RLS ও Android flow test coverage যাচাই।
4. Auth/session instrumentation, OTP provider configuration, leaked-password protection এবং remaining security warnings triage।
5. Android CI-র সর্বশেষ run ও artifact যাচাই; সফল artifact-কে debug/test build হিসেবেই চিহ্নিত করা।
