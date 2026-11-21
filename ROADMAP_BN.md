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

## বর্তমান অবস্থার সতর্কতা
বর্তমান repository-র Android APK একটি প্রাথমিক demo; এই নথিতে বর্ণিত login, backend, real jobs, admin, payment বা commission এখনো চালু হয়েছে—এমন দাবি করা হচ্ছে না। এই roadmap নিজে থেকে এসব feature বাস্তবায়ন করে না। প্রতিটি phase কোড, পরীক্ষা ও যাচাইয়ের মাধ্যমে সম্পন্ন করতে হবে।
